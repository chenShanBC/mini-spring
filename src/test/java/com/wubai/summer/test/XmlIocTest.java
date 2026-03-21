package com.wubai.summer.test;


import com.wubai.summer.core.ClassPathXmlApplicationContext;
import com.wubai.summer.test.Services.OrderService;
import com.wubai.summer.test.Services.UserService;

/**
 * XML配置方式的IoC容器测试
 */
public class XmlIocTest {
    public static void main(String[] args) {
        System.out.println("========== XML IoC 容器测试 ==========\n");

        // 1. 加载XML配置，启动容器
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("beans.xml");

        System.out.println("\n========== 测试获取Bean ==========");

        // 2. 按名称获取Bean
        UserService userService = (UserService) context.getBean("userService");
        System.out.println(" 获取到Bean：" + userService);

        OrderService orderService = (OrderService) context.getBean("orderService");
        System.out.println(" 获取到Bean：" + orderService);

        // 3. 测试依赖注入是否成功（如果UserService有方法可以验证）
        // userService.doSomething();

        System.out.println("\n========== 测试完成 ==========");
    }
}