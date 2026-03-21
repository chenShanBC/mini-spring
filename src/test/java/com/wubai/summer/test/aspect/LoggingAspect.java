package com.wubai.summer.test.aspect;


import com.wubai.summer.annotation.Component;
import com.wubai.summer.annotation.aop.After;
import com.wubai.summer.annotation.aop.Aspect;
import com.wubai.summer.annotation.aop.Before;

/**
 * 日志切面：在所有 Service 方法执行前打印日志
 */
@Component
@Aspect
public class LoggingAspect {
    @Before("*Service")  // 匹配所有以 Service 结尾的类
    public void logBefore() {
        System.out.println("【日志切面】方法执行前");
    }

    @After("*Service")
    public void logAfter() {
        System.out.println("【日志切面】方法执行后");
    }
}
