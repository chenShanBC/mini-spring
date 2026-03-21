package com.wubai.summer.core;

/**
 * @Author：fs
 * @Date:2026/3/517:52
 */

import com.wubai.summer.annotation.Order;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class ClassPathXmlApplicationContext {
    // 复用相同的三个核心缓存
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    private final Set<String> creatingBeanNames = ConcurrentHashMap.newKeySet();

    // 存储所有的 BeanPostProcessor
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    /**
     * 构造器：传入XML路径，启动容器
     *
     * @param xmlPath XML文件路径（相对于ClassPath，如 "beans.xml"）
     */
    public ClassPathXmlApplicationContext(String xmlPath) {
        System.out.println("【容器启动】正在加载XML配置文件：" + xmlPath);

        // 步骤1：解析XML，加载BeanDefinition
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader();
        this.beanDefinitionMap.putAll(reader.loadBeanDefinitions(xmlPath));

        // 步骤2：实例化所有Bean
        refresh();

        System.out.println("【容器启动】启动完成，共加载 " + singletonObjects.size() + " 个Bean实例");
    }

    /**
     * 刷新容器：实例化所有单例Bean
     */
    private void refresh() {
        // 第一阶段：先实例化所有 BeanPostProcessor
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDef = beanDefinitionMap.get(beanName);
            if (BeanPostProcessor.class.isAssignableFrom(beanDef.getBeanClass())) {
                BeanPostProcessor processor = (BeanPostProcessor) getBean(beanName);
                beanPostProcessors.add(processor);
        System.out.println("【Bean处理器】注册后置处理器：" + beanName);
            }
        }
        //- BeanPostProcessor 必须先于普通 Bean 实例化
        //- 这样普通 Bean 才能被 BeanPostProcessor 处理

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
     * 获取Bean实例（核心方法）
     * 与注解方式的区别：依赖按名称获取（getBean），而非按类型（getBeanByType）
     */
    public Object getBean(String beanName) {
        // 第一次检查：快速路径，无锁
        Object bean = singletonObjects.get(beanName);
        if (bean != null) {
            return bean;
        } else {
            // 获取BeanDefinition
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

                // 循环依赖检测
                if (!creatingBeanNames.add(beanName)) {
                    throw new RuntimeException("检测到循环依赖：" + beanName);
                }

                System.out.println("【Bean创建】正在实例化：" + beanName);

                // 实例化Bean
                Object instance;
                if (beanDef.getFactoryMethod() == null) {
                    instance = instantiateByConstructorXml(beanDef);
                } else {
                    instance = instantiateByFactoryMethod(beanDef);
                }

                //初始化 前
                // 属性注入
                injectPropertiesXml(instance, beanDef);
                // ⭐ 应用 BeanPostProcessor（初始化前）
                Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(instance, beanName);

                // TODO: 这里可以调用 init-method（如果支持）  自定义初始化方法

                // ⭐ 应用 BeanPostProcessor（初始化后）
                wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
                //初始化 后


                // 放入单例池（注意：放入的是处理后的 Bean）
                singletonObjects.put(beanName, wrappedBean);
                creatingBeanNames.remove(beanName);

                return wrappedBean;
            }
        }

    }

    /**
     * 构造器实例化（XML方式：根据constructorArgRefs按名称获取依赖）
     */
    private Object instantiateByConstructorXml(BeanDefinition beanDef) {
        try {
            Constructor<?> constructor = beanDef.getConstructor();
            List<String> argRefs = beanDef.getConstructorArgRefs();

            // 根据ref名称获取依赖Bean（递归调用getBean）
            Object[] paramValues = new Object[argRefs.size()];
            for (int i = 0; i < argRefs.size(); i++) {
                paramValues[i] = getBean(argRefs.get(i)); // 按名称获取
            }

            return constructor.newInstance(paramValues);
        } catch (Exception e) {
            throw new RuntimeException("构造器实例化失败：" + beanDef.getBeanName(), e);
        }
    }

    /**
     * 工厂方法实例化（与注解方式相同）
     */
    private Object instantiateByFactoryMethod(BeanDefinition beanDef) {
        try {
            Method factoryMethod = beanDef.getFactoryMethod();
            factoryMethod.setAccessible(true);
            Object factoryBean = getBean(beanDef.getFactoryBeanName());

            // 工厂方法无参数（简化版，可扩展支持参数）
            return factoryMethod.invoke(factoryBean);
        } catch (Exception e) {
            throw new RuntimeException("工厂方法实例化失败：" + beanDef.getBeanName(), e);
        }
    }

    /**
     * 属性注入（XML的<property>标签）
     * 根据属性名找到Setter方法，注入依赖Bean
     */
    private void injectPropertiesXml(Object instance, BeanDefinition beanDef) {
        Map<String, String> propertyRefs = beanDef.getPropertyRefs();
        if (propertyRefs.isEmpty()) return;

        Class<?> clazz = instance.getClass();
        for (Map.Entry<String, String> entry : propertyRefs.entrySet()) {
            String propertyName = entry.getKey();
            String refBeanName = entry.getValue();

            try {
                // 根据属性名生成Setter方法名（如 dataSource → setDataSource）
                String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

                // 获取依赖Bean
                Object refBean = getBean(refBeanName);

                // 查找匹配的Setter方法（参数类型与refBean兼容）
                Method setter = findSetter(clazz, setterName, refBean.getClass());
                setter.setAccessible(true);
                setter.invoke(instance, refBean);

                System.out.println("  └─ 注入属性：" + propertyName + " = " + refBeanName);

            } catch (Exception e) {
                throw new RuntimeException("属性注入失败：" + propertyName, e);
            }
        }
    }

    /**
     * 查找Setter方法（参数类型兼容即可）
     * 支持接口/父类注入（如参数是接口，实际注入实现类）
     */
    private Method findSetter(Class<?> clazz, String setterName, Class<?> paramType) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                Class<?> methodParamType = method.getParameterTypes()[0];
                // 检查类型兼容（支持接口/父类注入）
                if (methodParamType.isAssignableFrom(paramType)) {
                    return method;
                }
            }
        }
        throw new RuntimeException("找不到Setter方法：" + setterName);
    }

    /**
     * 按类型获取Bean（可选功能，如果需要支持按类型注入）
     */
    public <T> T getBeanByType(Class<T> type) {
        List<String> matchNames = new ArrayList<>();
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            if (type.isAssignableFrom(entry.getValue().getBeanClass())) {
                matchNames.add(entry.getKey());
            }
        }
        if (matchNames.isEmpty()) {
            throw new RuntimeException("无匹配类型的Bean：" + type.getName());
        }
        if (matchNames.size() > 1) {
            throw new RuntimeException("多个Bean匹配类型：" + type.getName());
        }
        return (T) getBean(matchNames.get(0));
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
}
/**
 * 1. **依赖解析方式不同**：XML按名称（getBean），注解按类型（getBeanByType）
 * 2. **构造器注入**：从constructorArgRefs读取Bean名称，递归调用getBean
 * 3. **属性注入**：从propertyRefs读取映射，找到Setter方法并反射调用
 * 4. **类型兼容检查**：支持接口/父类注入（isAssignableFrom）
 */
