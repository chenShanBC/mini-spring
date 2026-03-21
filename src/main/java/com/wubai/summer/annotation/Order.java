package com.wubai.summer.annotation;



import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定Bean的执行顺序（数字越小，优先级越高）
 * 主要用于 BeanPostProcessor 的排序
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Order {
    /**
     * 顺序值（数字越小，优先级越高）
     * 默认为 Integer.MAX_VALUE（最低优先级）
     */
    int value() default Integer.MAX_VALUE;
}