package com.wubai.summer.annotation.aop;



import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 前置通知：在方法执行前执行
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Before {
    /**
     * 切点表达式（简化版：只支持类名匹配）
     * 例如："*Service"表示所有以Service结尾的类
     */
    String value();  //`value()`：切点表达式，定义在哪些类/方法上生效
}
