package com.wubai.summer.test;

import com.wubai.summer.core.AnnotationConfigApplicationContext;
import com.wubai.summer.test.Services.IUserService;
import com.wubai.summer.test.Services.IOrderService;

/**
 * AOP Functionality Test
 */
public class AopTest {
    public static void main(String[] args) {
        System.out.println("========== AOP Functionality Test ==========\n");

        // 1. Start container (automatically scans and registers aspects)
        AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(
                com.wubai.summer.test.config.AppConfig.class
            );
        System.out.println("[INFO] IoC container started successfully\n");

        System.out.println("--- Test 1: AOP Proxy 创建 ---");

        // 2. Get Bean (should be a proxy object)
        IUserService userService = context.getBeanByType(IUserService.class);
        System.out.println("Bean type: " + userService.getClass().getName());
        boolean isProxy = userService.getClass().getName().contains("Proxy");
        System.out.println("Is proxy: " + isProxy);
        System.out.println();

        // 3. Call method (should trigger @Before aspect)
        System.out.println("--- Test 2: Invoke userService.sayHello() ---");
        userService.sayHello();
        System.out.println();

        // 4. Test OrderService
        System.out.println("--- Test 3: Invoke orderService.createOrder() ---");
        IOrderService orderService = context.getBeanByType(IOrderService.class);
        orderService.createOrder();

        System.out.println("\n========== All Tests Completed ==========");
    }
}
