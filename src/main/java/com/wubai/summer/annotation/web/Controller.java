package com.wubai.summer.annotation.web;

import com.wubai.summer.annotation.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author：fs
 * @Date:2026/3/818:00
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component  // 继承 @Component，自动被 IoC 扫描
public @interface Controller {
}
