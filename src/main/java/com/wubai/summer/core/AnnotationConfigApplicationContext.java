package com.wubai.summer.core;




import com.wubai.summer.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 注解配置的ApplicationContext：IoC容器核心
 */
public class AnnotationConfigApplicationContext {
    // 核心缓存1：Bean名称 → BeanDefinition（存储所有Bean的元数据）
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    // 核心缓存2：Bean名称 → Bean实例（单例池，存储初始化完成的Bean）
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    // 正在创建的Bean名称（解决构造器注入的循环依赖，检测后抛异常）
    private final Set<String> creatingBeanNames = new ConcurrentHashMap<>().newKeySet();
    // 存储所有的 BeanPostProcessor
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    // 构造器：传入配置类（如AppConfig），启动容器
    public AnnotationConfigApplicationContext(Class<?> configClass) {
        // 步骤1：扫描包，获取所有Class全限定名
        List<String> classNames = scan(configClass);
        // 步骤2：解析Class，注册BeanDefinition（核心）
        registerBeanDefinitions(classNames);
        // 步骤3：实例化所有单例Bean（含依赖注入、初始化）
        refresh();
    }

    /**
     * 包扫描：解析@ComponentScan，获取所有Class全限定名
     */
    private List<String> scan(Class<?> configClass) {
        // 1. 解析@ComponentScan注解，获取扫描路径    //通过类对象反射获取注解对象
        ComponentScan componentScan = configClass.getAnnotation(ComponentScan.class);
        String[] scanPackages;
        if (componentScan == null || componentScan.value().length == 0) {
            // 无注解   或为：无指定包，扫描配置类所在包   或为：扫描指定的包
            scanPackages = new String[]{configClass.getPackage().getName()};
        } else {
            // 扫描注解指定的包
            scanPackages = componentScan.value();
        }

        // 2. 遍历所有扫描包，调用ResourceResolver扫描Class，得到其所有全限定名
        List<String> allClassNames = new ArrayList<>();
        for (String pkg : scanPackages) {
            ResourceResolver resolver = new ResourceResolver(pkg);
            //把得到的指定包下的所有全限定名存在list中返回
            allClassNames.addAll(resolver.scanClassNames());
        }
        return allClassNames;
    }


    /**
     * 注册BeanDefinition：解析Class，将@Component/@Bean转换为BeanDefinition
     */
    private void registerBeanDefinitions(List<String> classNames) {
        for (String className : classNames) { //逐一处理每一个全限定名
            try {
                // 加载Class（根据全限定名得到其类对象）
                Class<?> clazz = Class.forName(className);
                // 过滤：仅处理带@Component注解的类（含@Configuration）
                Component component = clazz.getAnnotation(Component.class);
                if (!hasComponentAnnotation(clazz)) {
                    continue;
                }


                // 1. 注册普通Bean（@Component/@Configuration）
                String beanName = getDefaultBeanName(clazz, getComponentValue(Component.class));
                BeanDefinition beanDef = new BeanDefinition();
                beanDef.setBeanName(beanName);
                beanDef.setBeanClass(clazz);
                // 选择无参/有参构造器（优先带@Autowired的构造器，无则选无参）
                beanDef.setConstructor(selectConstructor(clazz));
                beanDefinitionMap.put(beanName, beanDef);

                // 2. 对@Configuration类，注册其@Bean方法为工厂Bean
                if (clazz.isAnnotationPresent(Configuration.class)) { //是 Java 反射中判断 “某个类 / 方法 / 字段上是否标注了指定注解” 的核心方法
                    registerBeanMethods(beanName, clazz);
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("加载Class失败：" + className, e);
            }
        }
    }

    /**
     * 注册@Bean方法：将配置类中的@Bean方法转换为BeanDefinition
     */
    private void registerBeanMethods(String factoryBeanName, Class<?> configClass) {
        // 遍历配置类的所有方法
        for (Method method : configClass.getMethods()) {
            Bean bean = method.getAnnotation(Bean.class);
            if (bean == null) continue;
            // 生成@Bean的Bean名称（方法名/注解指定名）
            String beanName = getDefaultBeanName(method, bean.value());
            BeanDefinition beanDef = new BeanDefinition();
            beanDef.setBeanName(beanName);
            beanDef.setBeanClass(method.getReturnType()); // 方法返回值为声明类型
            beanDef.setFactoryBeanName(factoryBeanName); // 所属配置类的Bean名称
            beanDef.setFactoryMethod(method); // 工厂方法

            beanDefinitionMap.put(beanName, beanDef);
        }
    }

    /**
     * 获取默认Bean名称：注解指定则用指定名，否则类名首字母小写/方法名
     */
    private String getDefaultBeanName(Class<?> clazz, String annoValue) {
        if (!annoValue.isEmpty()) return annoValue;
        String className = clazz.getSimpleName();
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    private String getDefaultBeanName(Method method, String annoValue) {
        if (!annoValue.isEmpty()) return annoValue;
        return method.getName();
    }

    /**
     * 选择构造器：优先带@Autowired的构造器，无则选无参构造器
     */
    private Constructor<?> selectConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        // 查找带@Autowired的构造器
        for (Constructor<?> c : constructors) {
            if (c.isAnnotationPresent(Autowired.class)) {
                c.setAccessible(true); // 开启访问权限（私有构造器也能实例化）
                return c;
            }
        }
        // 无则选无参构造器
        try {
            Constructor<?> noArgConstructor = clazz.getDeclaredConstructor();
            noArgConstructor.setAccessible(true);
            return noArgConstructor;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(clazz.getName() + "无@Autowired构造器且无无参构造器，无法实例化");
        }
    }

    /**
     * 刷新容器：实例化所有单例Bean，执行依赖注入和初始化
     */
    private void refresh() {
        // 第一阶段：先实例化所有 BeanPostProcessor
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDef = beanDefinitionMap.get(beanName);
            if (BeanPostProcessor.class.isAssignableFrom(beanDef.getBeanClass())) {
                BeanPostProcessor processor = (BeanPostProcessor) getBean(beanName);
                beanPostProcessors.add(processor);
            }
        }
        // 按 @Order 排序（数字越小，优先级越高）
        beanPostProcessors.sort((p1, p2) -> {
            int order1 = getOrder(p1);
            int order2 = getOrder(p2);
            return Integer.compare(order1, order2);
        }); //order1 order2 的顺序和p1，p2一致，就是从小到大


        // 第二阶段：实例化其他普通 Bean
        for (String beanName : beanDefinitionMap.keySet()) {
            getBean(beanName);
        }
    }

    /**
     * 核心方法：根据Bean名称获取Bean实例（不存在则实例化）
     */
    public Object getBean(String beanName) {
        // 1. 单例池中有则直接返回 , 第一次检查
        Object bean = singletonObjects.get(beanName);
        if (bean != null) {
            return bean;
        } else {
            // 2. 无则获取BeanDefinition，检测是否存在
            BeanDefinition beanDef = beanDefinitionMap.get(beanName);
            if (beanDef == null) {
                throw new RuntimeException("Bean不存在：" + beanName);
            }

            // 同步块：保证只有一个线程创建Bean
            synchronized (beanDef) {
                // 第二次检查：防止重复创建
                bean = singletonObjects.get(beanName);
                if (bean != null) {
                    return bean;
                }

                //-------创建这个单例bean的部分------
                // 3. 检测循环依赖（构造器注入）
                if (!creatingBeanNames.add(beanName)) {
                    throw new RuntimeException("检测到循环依赖：" + beanName);
                }

                // 4. 实例化Bean（分普通Bean和工厂Bean）
                Object instance;
                if (beanDef.getFactoryMethod() == null) {
                    // 普通Bean：@Component，用构造器实例化
                    instance = instantiateByConstructor(beanDef); //这个方法会将其所需要的所有依赖的对象注入进去，实现实例化
                } else {
                    // 工厂Bean：@Bean，用配置类实例+工厂方法实例化
                    instance = instantiateByFactoryMethod(beanDef);
                }
                beanDef.setInstance(instance);

                // 5. 依赖注入：Setter方法/字段注入（@Autowired）
                autowireBean(instance);

                // 6. 应用 BeanPostProcessor（初始化前）
                Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(instance, beanName);

                // TODO: 这里可以调用 init-method（如果支持）

                // 7. 应用 BeanPostProcessor（初始化后）
                wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);

                // 8. 放入单例池，移除创建中标记（注意：放入的是处理后的 Bean）
                singletonObjects.put(beanName, wrappedBean);
                creatingBeanNames.remove(beanName);

                return wrappedBean;
            }
        }


    }

    /**
     * 构造器实例化：普通Bean（@Component）
     */
    private Object instantiateByConstructor(BeanDefinition beanDef) {
        try {
            Constructor<?> constructor = beanDef.getConstructor();
            // 解析构造器参数（递归获取依赖的Bean）（获取构造器所需要的所有类的对应类对象）
            Class<?>[] paramTypes = constructor.getParameterTypes();
            Object[] paramValues = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                paramValues[i] = getBeanByType(paramTypes[i]); // 根据类型获取依赖Bean
            }
            // 反射创建实例
            return constructor.newInstance(paramValues);
        } catch (Exception e) {
            throw new RuntimeException("构造器实例化Bean失败：" + beanDef.getBeanName(), e);
        }
    }

    /**
     * 根据类型获取Bean（简化版：仅支持唯一类型匹配，无则抛异常）
     */
    public <T> T getBeanByType(Class<T> type) {
        // 遍历BeanDefinition，找到类型匹配的Bean
        List<BeanDefinition> matchDefs = beanDefinitionMap.values().stream()
                .filter(def -> type.isAssignableFrom(def.getBeanClass()))
                .collect(Collectors.toList()); //将满足的beanDefinition收集起来
        if (matchDefs.isEmpty()) {
            throw new RuntimeException("无匹配类型的Bean：" + type.getName());
        }
        if (matchDefs.size() > 1) {
            throw new RuntimeException("多个Bean匹配类型：" + type.getName());
        }
        // 获取Bean实例（第一个）（不存在则实例化）
        return (T) getBean(matchDefs.get(0).getBeanName());
    }


    /**
     * 工厂方法实例化：工厂Bean（@Bean）
     */
    private Object instantiateByFactoryMethod(BeanDefinition beanDef) {
        try {
            Method factoryMethod = beanDef.getFactoryMethod();
            factoryMethod.setAccessible(true);
            // 获取配置类实例（工厂Bean所属的@Configuration类）
            Object factoryBean = getBean(beanDef.getFactoryBeanName());
            // 解析工厂方法参数（递归获取依赖的Bean）
            Class<?>[] paramTypes = factoryMethod.getParameterTypes();
            Object[] paramValues = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                paramValues[i] = getBeanByType(paramTypes[i]);
            }
            // 反射调用工厂方法创建实例
            //为什么要返回获取配置类的实例：@bean标注的方法是实例方法，因此在beanDefinitionMap里面的实例Method，无法自行反射调用（实例要穿所属类别，静态才能null），所以才需要获取其配置类的实例来反射调用
            return factoryMethod.invoke(factoryBean, paramValues);
        } catch (Exception e) {
            throw new RuntimeException("工厂方法实例化Bean失败：" + beanDef.getBeanName(), e);
        }
    }

    /**
     * 依赖注入：对实例的Setter方法/字段执行@Autowired注入  (前面注入的是构造器需要的依赖，而setter和字段依赖是可选项，因此可能构造器中也没注入，总而言之，不做这一步得到的实例是半空壳实例)
     */
    private void autowireBean(Object instance) {
        Class<?> clazz = instance.getClass();
        // 1. 字段注入：遍历所有字段（含父类）
        injectFields(instance, clazz);
        // 2. Setter方法注入：遍历所有方法（含父类）
        injectMethods(instance, clazz);
    }

    /**
     * 字段注入：@Autowired标注的字段
     */
    private void injectFields(Object instance, Class<?> clazz) {
        //“递归停止 Object 类” ≠ “不处理类型为 Object 的字段”
        if (clazz == Object.class) return; // 递归到Object则停止 （递归父类到object）
        // 遍历当前类的所有字段
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                field.setAccessible(true);
                try {
                    // 根据字段类型获取依赖Bean，反射赋值
                    Object fieldValue = getBeanByType(field.getType());
                    field.set(instance, fieldValue);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("字段注入失败：" + clazz.getName() + "." + field.getName(), e);
                }
            }
        }
        // 递归扫描父类字段
        injectFields(instance, clazz.getSuperclass());
    }

    /**
     * Setter方法注入：@Autowired标注的setXxx方法
     */
    private void injectMethods(Object instance, Class<?> clazz) {
        if (clazz == Object.class) return;
        // 遍历当前类的所有方法
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Autowired.class) && method.getName().startsWith("set") && method.getParameterCount() == 1) {
                method.setAccessible(true);
                try {
                    // 根据方法参数类型获取依赖Bean，反射调用
                    Object paramValue = getBeanByType(method.getParameterTypes()[0]);
                    method.invoke(instance, paramValue);
                } catch (Exception e) {
                    throw new RuntimeException("Setter方法注入失败：" + clazz.getName() + "." + method.getName(), e);
                }
            }
        }
        // 递归扫描父类方法
        injectMethods(instance, clazz.getSuperclass());
    }

    // 新增：递归检查类是否标注了@Component（包括元注解）
    private boolean hasComponentAnnotation(Class<?> clazz) {
        // 1. 直接标注了@Component
        if (clazz.getAnnotation(Component.class) != null) {
            return true;
        }
        // 2. 遍历所有注解，检查是否有注解内部标注了@Component（元注解）
        for (Annotation anno : clazz.getAnnotations()) {
            if (anno.annotationType().getAnnotation(Component.class) != null) {
                return true;
            }
        }
        return false;
    }

    // 新增：获取@Component的value（兼容元注解）
    private String getComponentValue(Class<?> clazz) {
        // 1. 直接标注@Component
        Component component = clazz.getAnnotation(Component.class);
        if (component != null) {
            return component.value();
        }
        // 2. 从元注解中获取
        for (Annotation anno : clazz.getAnnotations()) {
            Component metaComponent = anno.annotationType().getAnnotation(Component.class);
            if (metaComponent != null) {
                return metaComponent.value();
            }
        }
        return "";
    }

    /**
     * 应用所有 BeanPostProcessor 的 postProcessBeforeInitialization
     */
    private Object applyBeanPostProcessorsBeforeInitialization(Object bean, String beanName) {
        Object result = bean;
        for (BeanPostProcessor processor : beanPostProcessors) {
            result = processor.postProcessBeforeInitialization(result, beanName);
            if (result == null) {
                throw new RuntimeException("BeanPostProcessor 返回了 null：" + processor.getClass().getName());
            }
        }
        return result;
    }

    /**
     * 应用所有 BeanPostProcessor 的 postProcessAfterInitialization
     */
    private Object applyBeanPostProcessorsAfterInitialization(Object bean, String beanName) {
        Object result = bean;
        for (BeanPostProcessor processor : beanPostProcessors) {
            result = processor.postProcessAfterInitialization(result, beanName);
            if (result == null) {
                throw new RuntimeException("BeanPostProcessor 返回了 null：" + processor.getClass().getName());
            }
        }
        return result;
    }

    /**
     * 获取 BeanPostProcessor 的 @Order 值
     */
    private int getOrder(BeanPostProcessor processor) {
        Order order = processor.getClass().getAnnotation(Order.class);
        return order != null ? order.value() : Integer.MAX_VALUE;
    }


    /**
     * 从ioc容器里遍历，获取所有带指定controller注解的 Bean
     */
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : singletonObjects.entrySet()) {
            Object bean = entry.getValue();
            // 检查 Bean 的类是否有指定注解
            if (bean.getClass().isAnnotationPresent(annotationType)) {
                result.put(entry.getKey(), bean);
            }
        }

        return result;
    }
}
