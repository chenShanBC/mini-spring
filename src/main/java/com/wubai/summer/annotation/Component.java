package com.wubai.summer.annotation;

import java.lang.annotation.*;


//@Component：标识被容器管理的 Bean
    //作用范围：
        // ElementType.TYPE 表示该注解只能标注在类（class）、接口（interface）、枚举（enum）
    //如果是 ElementType.METHOD 则只能标在方法上，ElementType.FIELD 只能标在字段上。
    @Target(ElementType.TYPE)

    //生命周期
        //RetentionPolicy.RUNTIME 表示注解会保留到运行时，程序可以通过反射（Reflection）获取到该注解的信息（这是 Spring 容器能扫描并识别 @Component 的关键）。
    //补充：
    //SOURCE：仅保留在源码中，编译后字节码文件中消失（如 @Override）；
    //CLASS：保留到编译后的字节码文件，但运行时无法通过反射获取。
    @Retention(RetentionPolicy.RUNTIME)

    @Documented  //作用：标记该注解会被 javadoc 工具提取到文档中（即生成 API 文档时，标注了 @Component 的类会显示该注解信息）。
public @interface Component {
    String value() default ""; //属性value，默认为“”
}
