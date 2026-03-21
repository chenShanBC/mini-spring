package com.wubai.summer.test.processor;

import com.wubai.summer.annotation.Component;
import com.wubai.summer.core.BeanPostProcessor;


@Component
public class LoggingBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        // 可选：记录 Bean 初始化日志
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 可选：记录 Bean 初始化完成日志
        return bean;
    }
}
