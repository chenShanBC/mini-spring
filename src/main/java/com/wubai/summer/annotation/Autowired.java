package com.wubai.summer.annotation;

import java.lang.annotation.*;


//@Autowired：标识依赖注入（构造器 / Setter / 字段）
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
}
