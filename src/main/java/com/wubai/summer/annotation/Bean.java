package com.wubai.summer.annotation;

import java.lang.annotation.*;


//@Bean：标识配置类中的工厂方法，创建第三方 Bean
@Target(ElementType.METHOD) //只能放在方法上
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {
    // Bean名称，默认空（后续用方法名）
    String value() default "";
}
