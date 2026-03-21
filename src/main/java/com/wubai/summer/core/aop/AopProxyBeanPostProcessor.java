package com.wubai.summer.core.aop;


import com.wubai.summer.annotation.Component;
import com.wubai.summer.annotation.aop.After;
import com.wubai.summer.annotation.aop.Around;
import com.wubai.summer.annotation.aop.Aspect;
import com.wubai.summer.annotation.aop.Before;
import com.wubai.summer.core.BeanPostProcessor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * AOP 代理处理器：为匹配的 Bean 创建代理对象
 */
@Component
public class AopProxyBeanPostProcessor implements BeanPostProcessor {
    // 存储所有切面信息
    // 这个不需要保证线程安全：
    //  核心原因是 IoC 容器初始化阶段是「单线程」执行的，且初始化完成后 aspects 只会被「读」，不会被「写」
    private final List<AspectInfo> aspects = new ArrayList<>();
    // 标记：是否已解析过切面（避免重复解析）
    private boolean isAspectParsed = false;
    // 新增：记录已解析的切面类，避免重复解析同一个切面
    private final List<Class<?>> parsedAspectClasses = new ArrayList<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        // 检查是否是切面类，如果是则立即解析（提前准备切面信息）
        if (bean.getClass().isAnnotationPresent(Aspect.class)) {
            // 避免重复解析同一个切面
            if (!parsedAspectClasses.contains(bean.getClass())) {
                parseAspect(bean);
                parsedAspectClasses.add(bean.getClass());
            }
        }
        return bean;
    }


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 切面类本身不需要代理，直接返回
        if (bean.getClass().isAnnotationPresent(Aspect.class)) {
            return bean;
        }

        // 检查是否需要为这个 Bean 创建代理
        if (shouldProxy(bean)) {
            return createProxy(bean);
        }
        return bean;
    }


    private void parseAspect(Object bean) {
        Method[] methods = bean.getClass().getMethods();
        for (Method method : methods) {
            // 解析 @Before
            if (method.isAnnotationPresent(Before.class)) {
                AspectInfo aspectInfo = new AspectInfo();
                aspectInfo.aspectBean = bean;   //切面bean是本身
                aspectInfo.adviceMethod = method; //通知方法
                Before before = method.getAnnotation(Before.class);
                aspectInfo.pointcut = before.value(); //切点表达式
                aspectInfo.adviceType = "before"; //通知类型
                aspects.add(aspectInfo);
            }

            // 解析 @After
            After after = method.getAnnotation(After.class);
            if (after != null) {
                AspectInfo info = new AspectInfo();
                info.aspectBean = bean;
                info.adviceMethod = method;
                info.pointcut = after.value();
                info.adviceType = "after";
                aspects.add(info);
            }

            // 解析 @Around
            if (method.isAnnotationPresent(Around.class)) {
                AspectInfo aspectInfo = new AspectInfo();
                aspectInfo.aspectBean = bean;
                aspectInfo.adviceMethod = method;
                Around around = method.getAnnotation(Around.class);
                aspectInfo.pointcut = around.value();
                aspectInfo.adviceType = "around";
                aspects.add(aspectInfo);
            }
        }
    }


    private boolean shouldProxy(Object bean) {
        String className = bean.getClass().getSimpleName();

        // 检查是否有原始bean类名匹配这个切面的切点
        for (AspectInfo aspect : aspects) {
            if (matches(className, aspect.pointcut)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 简单的匹配逻辑：支持 *Service 这样的通配符
     */
    private boolean matches(String className, String pointcut) {
        //前缀模糊 匹配
        if (pointcut.startsWith("*")) {
            // *Service -> 匹配所有以 Service 结尾的类
            String suffix = pointcut.substring(1); // *Service → 取 "Service"
            return className.endsWith(suffix); // UserService → 以 Service 结尾 → 匹配成功
        }

        // 精确匹配
        return className.equals(pointcut);
    }

    /**
     * 创建代理对象
     */
    private Object createProxy(Object bean) {
        // 基于JDK动态代理
        //实现逻辑：
        // 1、接口：JDK 代理只能代理「实现了接口的类」（代理类本质是实现该接口的子类）；
        // 2、Proxy类：JDK 原生工具类，用于生成代理类的字节码并创建代理对象；
        // 3、InvocationHandler接口：方法调用的拦截器，所有代理对象的方法调用都会走到该接口的invoke方法。

        // 步骤1：创建 InvocationHandler（和基础版一样抽成变量）
        // 步骤2：处理接口数组（兼容无接口）
        // 步骤3：生成代理对象（和基础版完全一致）
        InvocationHandler handler = new InvocationHandler() {
            @Override
            ///回调定义
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 检查是否有 @Around 通知
                AspectInfo aroundAspect = null;
                for (AspectInfo aspect : aspects) {
                    if (matches(bean.getClass().getSimpleName(), aspect.pointcut)) {
                        if ("around".equals(aspect.adviceType)) {
                            aroundAspect = aspect;
                            break;
                        }
                    }
                }
                // 如果有 @Around，由它控制整个流程
                        //环绕通知的核心定位：@Around 是 AOP 中优先级最高的通知类型，会完全接管被代理 Bean 方法的执行流程，若存在匹配的 @Around 通知，会直接跳过默认的「@Before → 原始方法 → @After」执行逻辑。
                        //ProceedingJoinPoint 的作用：作为环绕通知的核心载体，封装了被代理的原始 Bean、目标方法、方法入参，其 proceed() 方法是触发原始业务方法执行的唯一入口（调用则执行、不调用则原始方法不执行）。
                        //整体执行链路：解析加载 @Around 切面 → 匹配目标 Bean → 封装 ProceedingJoinPoint → 调用切面的环绕增强方法 → 增强方法内通过 proceed() 触发原始 Bean 方法执行 → 增强方法处理结果 / 异常后返回（全程由环绕通知控制
                if (aroundAspect != null) {
                    ProceedingJoinPoint joinPoint =
                            new ProceedingJoinPoint(bean, method, args);
                    return aroundAspect.adviceMethod.invoke(aroundAspect.aspectBean, joinPoint);
                }


                // 否则，执行 @Before -> 方法 -> @After
                for (AspectInfo aspect : aspects) {  //（为什么）要遍历切面？因为可能有多个切面都对这个方法进行了增强
                    if (matches(bean.getClass().getSimpleName(), aspect.pointcut)  //二次匹配的原因：排不掉不符合的增强的切面
                            && "before".equals(aspect.adviceType)) {
                        // 新增：设置方法可访问，避免私有方法调用报错
                        aspect.adviceMethod.setAccessible(true);
                        aspect.adviceMethod.invoke(aspect.aspectBean);
                    }
                }


                // 执行原始方法（和基础版完全一致）
                Object result = method.invoke(bean, args);

                // 执行 @After 通知
                for (AspectInfo aspect : aspects) {
                    if (matches(bean.getClass().getSimpleName(), aspect.pointcut)) {
                        if ("after".equals(aspect.adviceType)) {
                            aspect.adviceMethod.invoke(aspect.aspectBean);
                        }
                    }
                }

                return result;
            }
        };
        // 步骤2：处理接口数组（兼容无接口）
        Class<?>[] interfaces = bean.getClass().getInterfaces().length > 0
                ? bean.getClass().getInterfaces()
                : new Class[]{Object.class};
//所有类都默认继承 Object：Java 中任何类都是 Object 的子类，因此 Object.class 是所有类的「通用接口」（虽然 Object 是类，但 JDK 代理允许传入）；
//避免空数组报错：new Class[]{Object.class} 是「非空数组」，满足 JDK 代理的参数要求，不会抛出异常；
//最小侵入性：Object 是所有类的根，不会引入额外依赖，是最安全的兜底方案。


        // 步骤3：生成代理对象（和基础版完全一致） 通过类加载器
        return Proxy.newProxyInstance(
                bean.getClass().getClassLoader(),
                interfaces,
                handler);
    }
}