package com.wubai.summer.test.controller;

import com.wubai.summer.annotation.web.Controller;
import com.wubai.summer.annotation.web.GetMapping;
import com.wubai.summer.annotation.web.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author：fs
 * @Date:2026/3/818:20
 */
@Controller
public class UserController {

    @GetMapping("/hello")
    @ResponseBody
    public Map<String, Object> hello() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Hello, Summer MVC!");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    @GetMapping("/user/list")
    @ResponseBody
    public List<Map<String, Object>> getUserList() {
        List<Map<String, Object>> users = new ArrayList<>();

        Map<String, Object> user1 = new HashMap<>();
        user1.put("id", 1);
        user1.put("name", "张三");
        users.add(user1);

        Map<String, Object> user2 = new HashMap<>();
        user2.put("id", 2);
        user2.put("name", "李四");
        users.add(user2);
        return users;
    }
}
