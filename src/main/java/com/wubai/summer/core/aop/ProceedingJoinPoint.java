package com.wubai.summer.core.aop;



import java.lang.reflect.Method;

/**
 * 连接点：封装原始方法的信息
 */
public class ProceedingJoinPoint {
    private final Object target;      // 目标对象（原始bean对象）
    private final Method method;      // 目标方法
    private final Object[] args;      // 方法参数

    public ProceedingJoinPoint(Object target, Method method, Object[] args) {
        this.target = target;
        this.method = method;
        this.args = args;
    }

    /**
     * 执行原始方法
     */
    public Object proceed() throws Throwable {
        return method.invoke(target, args);
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }
}