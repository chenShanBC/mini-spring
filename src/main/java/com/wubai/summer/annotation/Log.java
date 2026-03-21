package com.wubai.summer.annotation;



import java.lang.annotation.*;

/**
 * 日志切面注解，标记需要打印日志的方法
 * @Log 只是贴在方法上的「标签」，本身不执行任何逻辑；
 * 具体提取该注解以实现日志逻辑要在对应应用类上实现 ：：： @Log 注解只是标记，真正的日志逻辑需要在「切面类」中提取注解信息并执行
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
}
