package com.wubai.summer.test;

import com.wubai.summer.core.AnnotationConfigApplicationContext;
import com.wubai.summer.test.Services.IUserService;
import com.wubai.summer.test.config.AppConfig;
import com.wubai.summer.test.pojo.User;

/**
 * Transaction Functionality Test
 */
public class TransactionTest {
    public static void main(String[] args) {
        System.out.println("========== Transaction Functionality Test ==========\n");

        // Create IoC container
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        System.out.println("[INFO] IoC container started successfully\n");

        IUserService userService = context.getBeanByType(IUserService.class);

        // Test 1: Normal commit
        System.out.println("--- Test 1: Normal Save (should commit) ---");
        try {
            User user1 = new User();
            user1.setName("Zhang San");
            user1.setAge(25);
            userService.saveUser(user1);
            System.out.println("[PASS] Save successful");
        } catch (Exception e) {
            System.out.println("[FAIL] Save failed: " + e.getMessage());
        }

        // Test 2: Exception rollback
        System.out.println("\n--- Test 2: Exception Save (should rollback) ---");
        try {
            User user2 = new User();
            user2.setName("Li Si");
            user2.setAge(30);
            userService.saveUserWithError(user2);
        } catch (Exception e) {
            System.out.println("[PASS] Save failed (expected behavior): " + e.getMessage());
        }

        System.out.println("\n========== All Tests Completed ==========");
    }
}