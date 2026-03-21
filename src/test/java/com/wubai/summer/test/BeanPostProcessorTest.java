package com.wubai.summer.test;

import com.wubai.summer.core.AnnotationConfigApplicationContext;
import com.wubai.summer.test.Services.IUserService;
import com.wubai.summer.test.config.AppConfig;


public class BeanPostProcessorTest {
    public static void main(String[] args) {
        System.out.println("========== BeanPostProcessor Test ==========\n");

        // Start container (automatically registers BeanPostProcessor)
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(AppConfig.class);
        System.out.println("[INFO] IoC container started successfully\n");

        System.out.println("--- Test: Get and Invoke Bean ---");

        // Get Bean (may be a proxy object)
        IUserService userService = context.getBeanByType(IUserService.class);
        System.out.println("Bean type: " + userService.getClass().getName());

        // Invoke method (if proxied, will see proxy logs)
        userService.sayHello();

        System.out.println("\n========== Test Completed ==========");
    }
}
