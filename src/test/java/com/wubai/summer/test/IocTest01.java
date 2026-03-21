package com.wubai.summer.test;

import com.wubai.summer.core.AnnotationConfigApplicationContext;
import com.wubai.summer.test.Services.IOrderService;
import com.wubai.summer.test.Services.IUserService;
import com.wubai.summer.test.config.AppConfig;
import com.wubai.summer.test.config.DataSource01;


public class IocTest01 {
    public static void main(String[] args) {
        System.out.println("========== IoC Container Test ==========\n");

        // 1. Start IoC container
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        System.out.println("[INFO] IoC container started successfully\n");

        // 2. Get Bean by type (using interface)
        System.out.println("--- Test 1: Get Bean by Type ---");
        IUserService userService = context.getBeanByType(IUserService.class);
        System.out.println("Bean type: " + userService.getClass().getName());
        userService.sayHello();
        System.out.println();

        // 3. Get another Bean by type
        System.out.println("--- Test 2: Get OrderService Bean ---");
        IOrderService orderService = context.getBeanByType(IOrderService.class);
        orderService.createOrder();
        System.out.println();

        // 4. Get third-party Bean (created by @Bean)
        System.out.println("--- Test 3: Get Third-party Bean ---");
        DataSource01 dataSource = context.getBeanByType(DataSource01.class);
        System.out.println("DataSource: " + dataSource);
        System.out.println();

        // 5. Verify singleton pattern
        System.out.println("--- Test 4: Verify Singleton ---");
        IUserService userService2 = context.getBeanByType(IUserService.class);
        boolean isSingleton = (userService == userService2);
        System.out.println("Same instance: " + isSingleton);
        System.out.println("Expected: true, Actual: " + isSingleton);

        if (isSingleton) {
            System.out.println("[PASS] Singleton test passed");
        } else {
            System.out.println("[FAIL] Singleton test failed");
        }

        System.out.println("\n========== All Tests Completed ==========");
    }
}
