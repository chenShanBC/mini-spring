package com.wubai.summer.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wubai.summer.annotation.web.Controller;
import com.wubai.summer.annotation.web.GetMapping;
import com.wubai.summer.annotation.web.ResponseBody;
import com.wubai.summer.core.AnnotationConfigApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


//「接收所有 HTTP 请求 → 解析 URL → 分发到对应 Controller 方法 → 处理返回值并响应」
    //DispatcherServlet 是「前端控制器（Front Controller）」，是所有 HTTP 请求的「统一入口」：
        //替代了传统 Servlet 每个接口写一个 Servlet 的繁琐方式；
        //负责 URL 和 Controller 方法的映射、方法调用、返回值处理；
        //是手写 MVC 模块的「核心中枢」，串联起 IoC 容器和 Web 请求。
public class DispatcherServlet extends HttpServlet {
    // 1. IoC 容器：获取所有 @Controller 实例（复用之前手写的 IoC 核心）
    private AnnotationConfigApplicationContext context;
    // 2. URL 映射表：key=请求URL，value=对应的Controller方法信息（核心映射关系）
    private Map<String, HandlerInfo> urlMappings = new HashMap<>();
    // 3. JSON 序列化工具：将返回值转为 JSON 字符串（Spring MVC 内置 Jackson）
    private ObjectMapper objectMapper = new ObjectMapper();


    // 内部类：封装 Controller 方法信息
    private static class HandlerInfo {
        Object controller;
        Method method;

        HandlerInfo(Object controller, Method method) {
            this.controller = controller;
            this.method = method;
        }
    }


    //为什么 init () 只执行一次？（Servlet 生命周期：init () 在容器启动时执行，destroy () 在销毁时执行，service/doGet/doPost 每次请求执行）；
    @Override
    public void init() throws ServletException {
        try {
            // 1. 启动 IoC 容器
            Class<?> configClass = Class.forName("com.wubai.summer.test.config.AppConfig");
            context = new AnnotationConfigApplicationContext(configClass);

            // 2. 扫描所有 @Controller  //为什么从 IoC 容器拿 Controller？（Controller 是 IoC 管理的 Bean，支持依赖注入，比如 Controller 中 @Autowired Service）
            Map<String, Object> controllers = context.getBeansWithAnnotation(Controller.class);

            // 3. 注册 URL 映射
            for (Object controller : controllers.values()) {
                registerController(controller);
            }
        } catch (Exception e) {
            throw new ServletException("初始化失败", e);
        }
    }

    private void registerController(Object controller) {
        for (Method method : controller.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(GetMapping.class)) {
                String url = method.getAnnotation(GetMapping.class).value();
                urlMappings.put(url, new HandlerInfo(controller, method));
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String url = req.getRequestURI();

        HandlerInfo handler = urlMappings.get(url);
        if (handler == null) {
            resp.setStatus(404);
            resp.getWriter().write("{\"error\":\"404 Not Found\"}");
            return;
        }

        try {
            // 调用 Controller 方法
            Object result = handler.method.invoke(handler.controller);

            // 返回 JSON
            if (handler.method.isAnnotationPresent(ResponseBody.class)) {
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write(objectMapper.writeValueAsString(result));
            } else {
                resp.getWriter().write(String.valueOf(result));
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}

//这个 DispatcherServlet 是我手写 MVC 模块的核心，参考了 Spring MVC 的前端控制器设计：
    //初始化阶段：复用手写的 IoC 容器加载所有 @Controller，解析 @GetMapping 构建 URL 映射表，避免每次请求都扫描，提升性能；
    //请求处理阶段：接收 GET 请求后，通过 URL 匹配对应的 Controller 方法，反射调用后处理返回值 —— 标注 @ResponseBody 就返回 JSON，否则返回字符串；
    //目前实现了核心流程，还可以优化：比如支持 POST 请求、PathVariable 参数、统一异常处理等，这些都是 Spring MVC 官方的核心特性。
