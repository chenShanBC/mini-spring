package com.wubai.summer.core.aop;


public class AspectInfo {
    public Object aspectBean;       // 切面实例
    public String pointcut;         // 切点表达式
    public String adviceType;       // 通知类型（before/after）
    public java.lang.reflect.Method adviceMethod; // 通知方法
}