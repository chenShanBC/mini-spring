package com.wubai.summer.annotation;

import java.lang.annotation.*;


// @ComponentScan：指定包扫描路径
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
public @interface ComponentScan {
        //扫描的包路径，默认空（后续使用配置类所在的包）
    String[] value() default {};
}
