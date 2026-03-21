package com.wubai.summer.core;


import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bean定义：存储Bean的元数据（设计图纸）
 */
//核心作用：存储 Bean 的所有元数据（设计图纸），代替直接存储 Bean 实例，方便后续实例化和依赖注入。
public class BeanDefinition {
    // Bean的唯一名称
    private String beanName;
    // Bean的Class类型（声明类型）
    private Class<?> beanClass;

    //Bean的实例（初始化后赋值）初始为 null
        //  ---支持 “延迟实例化”，提升容器性能
        //---支持懒加载：可以做到 “第一次获取 Bean 时才实例化”（比如@Lazy注解的实现基础）
        //---解决循环依赖：容器先收集所有 Bean 的元数据，再按依赖顺序实例化，避免 “创建 A 时需要 B，而 B 还没创建” 的问题。
    private Object instance;
    // 构造器（用于实例化@Component的Bean）
    private Constructor<?> constructor;
    // 工厂Bean名称（@Bean方法所属的@Configuration类名）
    private String factoryBeanName;
    // 工厂方法（用于实例化@Bean的Bean）
    private Method factoryMethod;

    // ========== 新增：XML配置专用字段 ==========
// XML专用：构造器参数的Bean引用列表（按顺序存储ref，如["orderService", "dataSource"]）
    private List<String> constructorArgRefs = new ArrayList<>();

    // XML专用：属性注入的Bean引用映射（属性名 → Bean名称，如 {"dataSource": "dataSource"}）
    private Map<String, String> propertyRefs = new HashMap<>();


    // 无参构造器
    public BeanDefinition() {}

    // 注解版本 全参构造器（简化赋值）
    public BeanDefinition(String beanName, Class<?> beanClass, Object instance, Constructor<?> constructor, String factoryBeanName, Method factoryMethod) {
        this.beanName = beanName;
        this.beanClass = beanClass;
        this.instance = instance;
        this.constructor = constructor;
        this.factoryBeanName = factoryBeanName;
        this.factoryMethod = factoryMethod;
    }

    // Getter/Setter（必须，后续反射赋值）
    public String getBeanName() { return beanName; }
    public void setBeanName(String beanName) { this.beanName = beanName; }
    public Class<?> getBeanClass() { return beanClass; }
    public void setBeanClass(Class<?> beanClass) { this.beanClass = beanClass; }
    public Object getInstance() { return instance; }
    public void setInstance(Object instance) { this.instance = instance; }
    public Constructor<?> getConstructor() { return constructor; }
    public void setConstructor(Constructor<?> constructor) { this.constructor = constructor; }
    public String getFactoryBeanName() { return factoryBeanName; }
    public void setFactoryBeanName(String factoryBeanName) { this.factoryBeanName = factoryBeanName; }
    public Method getFactoryMethod() { return factoryMethod; }
    public void setFactoryMethod(Method factoryMethod) { this.factoryMethod = factoryMethod; }


    // ========== 新增：XML字段的Getter/Setter ==========
    public List<String> getConstructorArgRefs() {
        return constructorArgRefs;
    }

    public void setConstructorArgRefs(List<String> constructorArgRefs) {
        this.constructorArgRefs = constructorArgRefs;
    }

    public Map<String, String> getPropertyRefs() {
        return propertyRefs;
    }

    public void setPropertyRefs(Map<String, String> propertyRefs) {
        this.propertyRefs = propertyRefs;
    }
}
