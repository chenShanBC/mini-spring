package com.wubai.summer.core;


//创建一个接口，定义两个钩子方法：
public interface BeanPostProcessor {
    /**
     * Bean 初始化之前调用
     * @param bean 原始 Bean 实例
     * @param beanName Bean 名称
     * @return 处理后的 Bean（可以是原始 Bean 或包装后的 Bean）
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;  // 默认返回原始 Bean
    }

    /**
     * Bean 初始化之后调用（通常在这里做代理替换）
     * @param bean 原始 Bean 实例
     * @param beanName Bean 名称
     * @return 处理后的 Bean（可以是原始 Bean 或代理对象）
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;  // 默认返回原始 Bean
    }
}
