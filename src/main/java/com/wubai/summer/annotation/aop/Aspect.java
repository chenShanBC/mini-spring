package com.wubai.summer.annotation.aop;



import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个类为切面
 */
@Target(ElementType.TYPE) //：只能用在类上  /// 【接口、枚举】也行的
@Retention(RetentionPolicy.RUNTIME)
public @interface Aspect {

}
