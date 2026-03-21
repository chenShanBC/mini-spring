package com.wubai.summer.test.aspect;

import com.wubai.summer.annotation.Component;
import com.wubai.summer.annotation.aop.Around;
import com.wubai.summer.annotation.aop.Aspect;
import com.wubai.summer.core.aop.ProceedingJoinPoint;

@Aspect
@Component
public class PerformanceAspect {


    @Around("*Service")
    public Object measurePerformance(ProceedingJoinPoint joinPoint)
            throws Throwable {
        String methodName = joinPoint.getMethod().getName();

        System.out.println("【性能监控】开始执行：" + methodName);
        long start = System.currentTimeMillis();

        // 执行原始方法
        Object result = joinPoint.proceed();

        long end = System.currentTimeMillis();
        System.out.println("【性能监控】执行耗时：" + (end - start) + "毫秒");

        return result;
    }
}