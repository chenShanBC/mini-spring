package com.wubai.summer.boot;

import com.wubai.summer.web.DispatcherServlet;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

public class Application {
    public static void main(String[] args) throws Exception {
        // 1. 创建 Tomcat 服务器实例（核心：编程式启动 Tomcat，而非传统配置）
        Tomcat tomcat = new Tomcat();

        // 2. 设置 Tomcat 监听端口（默认 8080，可自定义）
        tomcat.setPort(8080);

        // 3. 初始化 Tomcat 连接器（Connector）：负责处理 TCP 连接、HTTP 协议解析
        // 注：tomcat.getConnector() 会懒加载创建连接器，必须调用（否则启动后端口不通）
        tomcat.getConnector();

        // 4. 创建 Web 应用上下文（Context）：对应传统 Tomcat 的 web.xml 上下文配置
        // 参数1：Context 路径（"" 表示根路径，即 http://localhost:8080/）；参数2：webapp 目录（null 表示无需物理目录）
        Context context = tomcat.addContext("", null);

        // 5. 注册 DispatcherServlet 到 Tomcat 上下文
        // 参数1：上下文；参数2：Servlet 名称（自定义）；参数3：Servlet 实例（我们手写的 DispatcherServlet）
        Tomcat.addServlet(context, "dispatcher", new DispatcherServlet());

        // 6. 映射 Servlet：将所有请求（"/"）都转发给名为 "dispatcher" 的 Servlet 处理
        // 核心：实现「所有请求都经过 DispatcherServlet」（前端控制器模式）
        context.addServletMappingDecoded("/", "dispatcher");

        // 7. 启动 Tomcat 服务器（启动核心线程、监听端口、接收请求）
        tomcat.start();
        System.out.println("【服务器】启动成功，访问地址：http://localhost:8080");

        // 8. 阻塞主线程：让 Tomcat 一直运行（否则 main 方法执行完就退出，服务器立即关闭）
        tomcat.getServer().await();
    }
}