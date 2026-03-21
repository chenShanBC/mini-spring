[架构说明文档.md](https://github.com/user-attachments/files/26155447/default.md)
# Summer Framework 架构说明文档

## 项目概述

Summer Framework 是一个轻量级的 Java 应用框架，参考 Spring Framework 设计，实现了 IoC 容器、AOP、事务管理、Web MVC 等核心功能。

---

## 一、核心功能模块

### 1. IoC 容器（依赖注入）

#### 涉及的核心类

| 类名 | 路径 | 职责 |
|------|------|------|
| `AnnotationConfigApplicationContext` | `core/` | IoC 容器核心，负责 Bean 的扫描、注册、实例化、依赖注入 |
| `BeanDefinition` | `core/` | Bean 元数据，存储 Bean 的类型、名称、构造器、工厂方法等信息 |
| `ResourceResolver` | `core/` | 包扫描工具，扫描指定包下的所有 Class 文件 |
| `BeanPostProcessor` | `core/` | Bean 后置处理器接口，用于扩展 Bean 初始化流程 |
| `ClassPathXmlApplicationContext` | `core/` | XML 配置方式的 IoC 容器 |
| `XmlBeanDefinitionReader` | `core/` | XML 配置文件解析器 |

#### 相关注解

| 注解 | 路径 | 作用 |
|------|------|------|
| `@Component` | `annotation/` | 标记类为 Spring 管理的组件 |
| `@Configuration` | `annotation/` | 标记配置类（元注解包含 @Component） |
| `@ComponentScan` | `annotation/` | 指定包扫描路径 |
| `@Bean` | `annotation/` | 在配置类中定义 Bean |
| `@Autowired` | `annotation/` | 标记需要自动注入的字段/构造器/方法 |
| `@Order` | `annotation/` | 指定 Bean 的加载顺序 |

#### 调用链

```
启动流程：
1. new AnnotationConfigApplicationContext(AppConfig.class)
   ↓
2. scan(configClass)  // 包扫描
   ├─ 解析 @ComponentScan 注解获取扫描包路径
   ├─ ResourceResolver.scanClassNames()  // 扫描所有 Class
   └─ 返回 List<String> classNames
   ↓
3. registerBeanDefinitions(classNames)  // 注册 BeanDefinition
   ├─ 遍历每个 className
   ├─ Class.forName(className) 加载类
   ├─ 检查是否有 @Component 注解（含元注解）
   ├─ 创建 BeanDefinition 并存入 beanDefinitionMap
   └─ 如果是 @Configuration 类，调用 registerBeanMethods()
       └─ 遍历 @Bean 方法，为每个方法创建 BeanDefinition
   ↓
4. refresh()  // 实例化所有单例 Bean
   ├─ 第一阶段：实例化所有 BeanPostProcessor
   │   └─ 按 @Order 排序
   ├─ 第二阶段：实例化其他普通 Bean
   │   └─ 遍历 beanDefinitionMap，调用 getBean(beanName)
   ↓
5. getBean(beanName)  // 获取/创建 Bean
   ├─ 检查单例池 singletonObjects，存在则直接返回
   ├─ 不存在则创建：
   │   ├─ 检测循环依赖（creatingBeanNames）
   │   ├─ 实例化 Bean：
   │   │   ├─ 普通 Bean：instantiateByConstructor()
   │   │   │   ├─ 获取构造器参数类型
   │   │   │   ├─ 递归调用 getBeanByType() 获取依赖
   │   │   │   └─ 反射创建实例
   │   │   └─ 工厂 Bean：instantiateByFactoryMethod()
   │   │       ├─ 获取配置类实例（递归 getBean）
   │   │       ├─ 解析方法参数（递归 getBeanByType）
   │   │       └─ 反射调用工厂方法
   │   ├─ 依赖注入：autowireBean(instance)
   │   │   ├─ injectFields()  // 字段注入
   │   │   └─ injectMethods()  // Setter 方法注入
   │   ├─ 应用 BeanPostProcessor（初始化前）
   │   ├─ 应用 BeanPostProcessor（初始化后）
   │   └─ 放入单例池
   └─ 返回 Bean 实例
```

#### 关键设计

- **单例池**：`Map<String, Object> singletonObjects` 缓存已创建的 Bean
- **BeanDefinition 缓存**：`Map<String, BeanDefinition> beanDefinitionMap` 存储 Bean 元数据
- **循环依赖检测**：`Set<String> creatingBeanNames` 检测构造器注入的循环依赖
- **双重检查锁**：`synchronized (beanDef)` 保证线程安全

---

### 2. AOP（面向切面编程）

#### 涉及的核心类

| 类名 | 路径 | 职责 |
|------|------|------|
| `AopProxyBeanPostProcessor` | `core/aop/` | AOP 代理创建器，实现 BeanPostProcessor 接口 |
| `AspectInfo` | `core/aop/` | 切面信息封装，存储切点表达式和通知方法 |
| `ProceedingJoinPoint` | `core/aop/` | 连接点对象，用于 @Around 通知 |

#### 相关注解

| 注解 | 路径 | 作用 |
|------|------|------|
| `@Aspect` | `annotation/aop/` | 标记切面类 |
| `@Before` | `annotation/aop/` | 前置通知 |
| `@After` | `annotation/aop/` | 后置通知 |
| `@Around` | `annotation/aop/` | 环绕通知 |

#### 调用链

```
AOP 代理创建流程：
1. IoC 容器启动时，AopProxyBeanPostProcessor 被注册为 BeanPostProcessor
   ↓
2. 每个 Bean 初始化后，调用 postProcessAfterInitialization()
   ↓
3. AopProxyBeanPostProcessor.postProcessAfterInitialization(bean, beanName)
   ├─ 从 IoC 容器获取所有 @Aspect 切面类
   ├─ 遍历切面类的所有方法，查找 @Before/@After/@Around 注解
   ├─ 解析切点表达式（如 "*Service"）
   ├─ 判断当前 Bean 是否匹配切点表达式
   └─ 如果匹配，创建 JDK 动态代理：
       └─ Proxy.newProxyInstance(classLoader, interfaces, invocationHandler)
           └─ InvocationHandler.invoke() 中：
               ├─ 执行 @Before 通知
               ├─ 执行目标方法 method.invoke(target, args)
               ├─ 执行 @After 通知
               └─ 返回结果
   ↓
4. 返回代理对象（或原始对象）
```

#### 关键设计

- **JDK 动态代理**：要求目标类实现接口
- **切点匹配**：简单的字符串匹配（类名包含切点表达式）
- **通知执行顺序**：@Before → 目标方法 → @After
- **@Around 通知**：通过 `ProceedingJoinPoint.proceed()` 控制目标方法执行

---

### 3. 事务管理

#### 涉及的核心类

| 类名 | 路径 | 职责 |
|------|------|------|
| `TransactionManager` | `jdbc/` | 事务管理器，管理数据库连接和事务状态 |
| `TransactionAspect` | `jdbc/` | 事务切面，拦截 @Transactional 方法 |
| `JdbcTemplate` | `jdbc/` | JDBC 模板类，简化数据库操作 |
| `DataSourceConfig` | `jdbc/` | 数据源配置类 |

#### 相关注解

| 注解 | 路径 | 作用 |
|------|------|------|
| `@Transactional` | `annotation/tx/` | 声明式事务注解 |

#### 调用链

```
事务执行流程：
1. 用户调用带 @Transactional 注解的方法
   ↓
2. AOP 代理拦截调用
   ↓
3. TransactionAspect.aroundTransaction(ProceedingJoinPoint pjp)
   ├─ TransactionManager.beginTransaction()
   │   ├─ 从 ThreadLocal 获取当前线程的连接
   │   ├─ 如果没有连接，从数据源获取新连接
   │   ├─ connection.setAutoCommit(false)  // 关闭自动提交
   │   └─ 将连接存入 ThreadLocal
   ├─ 执行目标方法：pjp.proceed()
   ├─ 如果成功：TransactionManager.commit()
   │   ├─ connection.commit()
   │   ├─ connection.close()
   │   └─ ThreadLocal.remove()
   └─ 如果异常：TransactionManager.rollback()
       ├─ connection.rollback()
       ├─ connection.close()
       ├─ ThreadLocal.remove()
       └─ 重新抛出异常
```

#### 关键设计

- **ThreadLocal 连接管理**：保证同一线程内的多个 DAO 操作使用同一个连接
- **声明式事务**：通过 AOP 实现，无需手动编写事务代码
- **异常回滚**：捕获异常后自动回滚事务

---

### 4. Web MVC

#### 涉及的核心类

| 类名 | 路径 | 职责 |
|------|------|------|
| `DispatcherServlet` | `web/` | 前端控制器，处理所有 HTTP 请求 |
| `Application` | `boot/` | 内嵌 Tomcat 启动类 |

#### 相关注解

| 注解 | 路径 | 作用 |
|------|------|------|
| `@Controller` | `annotation/web/` | 标记控制器类 |
| `@GetMapping` | `annotation/web/` | 映射 GET 请求 |
| `@ResponseBody` | `annotation/web/` | 返回 JSON 响应 |

#### 调用链

```
Web 请求处理流程：
1. Application.main() 启动
   ├─ 创建 Tomcat 实例
   ├─ 设置端口 8080
   ├─ 创建 Web 应用上下文
   ├─ 注册 DispatcherServlet
   ├─ 映射所有请求到 DispatcherServlet
   └─ 启动 Tomcat
   ↓
2. DispatcherServlet.init()  // Servlet 初始化（只执行一次）
   ├─ 启动 IoC 容器：new AnnotationConfigApplicationContext(AppConfig.class)
   ├─ 获取所有 @Controller Bean
   ├─ 遍历 Controller 的所有方法
   ├─ 查找 @GetMapping 注解
   ├─ 构建 URL 映射表：Map<String, HandlerInfo>
   │   └─ HandlerInfo { controller, method }
   └─ 打印注册的映射
   ↓
3. 用户发起 HTTP GET 请求：http://localhost:8080/user/list
   ↓
4. DispatcherServlet.doGet(request, response)
   ├─ 获取请求 URI：request.getRequestURI()
   ├─ 从 urlMappings 查找对应的 HandlerInfo
   ├─ 如果找不到，返回 404
   ├─ 如果找到：
   │   ├─ 反射调用 Controller 方法：method.invoke(controller)
   │   ├─ 获取返回值
   │   └─ 判断是否有 @ResponseBody 注解：
   │       ├─ 有：使用 Jackson 序列化为 JSON
   │       └─ 无：直接返回字符串
   └─ 写入响应：response.getWriter().write(result)
```

#### 关键设计

- **前端控制器模式**：所有请求统一由 DispatcherServlet 处理
- **URL 映射表**：`Map<String, HandlerInfo>` 存储 URL 和方法的映射关系
- **反射调用**：通过反射动态调用 Controller 方法
- **JSON 序列化**：使用 Jackson 将对象转为 JSON

---

### 5. JDBC 模板

#### 涉及的核心类

| 类名 | 路径 | 职责 |
|------|------|------|
| `JdbcTemplate` | `jdbc/` | JDBC 模板类，封装常用数据库操作 |
| `DataSourceConfig` | `jdbc/` | 数据源配置（Druid 连接池） |

#### 调用链

```
JDBC 操作流程：
1. 配置类中创建 JdbcTemplate Bean
   @Bean
   public JdbcTemplate jdbcTemplate(DataSource dataSource) {
       return new JdbcTemplate(dataSource);
   }
   ↓
2. DAO 层注入 JdbcTemplate
   @Autowired
   private JdbcTemplate jdbcTemplate;
   ↓
3. 执行查询：jdbcTemplate.query(sql, rowMapper, params)
   ├─ TransactionManager.getConnection()  // 获取连接（支持事务）
   ├─ connection.prepareStatement(sql)
   ├─ 设置参数：ps.setObject(i, param)
   ├─ 执行查询：ps.executeQuery()
   ├─ 遍历结果集：while (rs.next())
   │   └─ rowMapper.mapRow(rs, rowNum)  // 映射为对象
   └─ 返回 List<T>
   ↓
4. 执行更新：jdbcTemplate.update(sql, params)
   ├─ TransactionManager.getConnection()
   ├─ connection.prepareStatement(sql)
   ├─ 设置参数
   ├─ 执行更新：ps.executeUpdate()
   └─ 返回影响行数
```

#### 关键设计

- **模板方法模式**：封装 JDBC 样板代码
- **RowMapper 接口**：自定义结果集映射
- **事务集成**：通过 TransactionManager 获取连接，支持事务

---

## 二、完整调用链示例

### 示例：带事务的 Web 请求

```
用户请求：POST /user/save
   ↓
1. Tomcat 接收请求，转发给 DispatcherServlet
   ↓
2. DispatcherServlet.doPost()
   ├─ 查找 URL 映射：/user/save → UserController.saveUser()
   └─ 反射调用：method.invoke(userController)
   ↓
3. UserController.saveUser()  // 这是一个代理对象
   └─ 调用 userService.createUser(user)
   ↓
4. AOP 代理拦截（因为 UserService 有 @Transactional）
   └─ TransactionAspect.aroundTransaction()
       ├─ TransactionManager.beginTransaction()  // 开启事务
       ├─ 执行目标方法：userService.createUser(user)
       │   ↓
       │   UserService.createUser(user)
       │   └─ userDao.insert(user)
       │       ↓
       │       UserDao.insert(user)
       │       └─ jdbcTemplate.update(sql, params)
       │           ├─ TransactionManager.getConnection()  // 获取事务连接
       │           ├─ 执行 SQL：INSERT INTO user ...
       │           └─ 返回影响行数
       ├─ TransactionManager.commit()  // 提交事务
       └─ 返回结果
   ↓
5. DispatcherServlet 处理返回值
   ├─ 检查 @ResponseBody 注解
   ├─ 序列化为 JSON：{"success": true}
   └─ 写入响应
   ↓
6. 返回给客户端
```

---

## 三、核心设计模式

| 设计模式 | 应用场景 | 实现类 |
|---------|---------|--------|
| **工厂模式** | Bean 的创建 | `AnnotationConfigApplicationContext` |
| **单例模式** | Bean 的单例管理 | `singletonObjects` 缓存 |
| **代理模式** | AOP 实现 | `AopProxyBeanPostProcessor` |
| **模板方法模式** | JDBC 操作 | `JdbcTemplate` |
| **前端控制器模式** | Web 请求分发 | `DispatcherServlet` |
| **策略模式** | Bean 实例化策略 | 构造器实例化 vs 工厂方法实例化 |
| **观察者模式** | Bean 生命周期扩展 | `BeanPostProcessor` |

---

## 四、扩展点

### 1. BeanPostProcessor

自定义 Bean 后置处理器，可以在 Bean 初始化前后执行自定义逻辑：

```java
@Component
@Order(1)
public class CustomBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        // Bean 初始化前的逻辑
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // Bean 初始化后的逻辑（如创建 AOP 代理）
        return bean;
    }
}
```

### 2. 自定义切面

```java
@Aspect
@Component
public class LoggingAspect {
    @Before("*Service")
    public void logBefore() {
        System.out.println("方法执行前...");
    }
}
```

### 3. 自定义注解

可以创建元注解，组合 `@Component`：

```java
@Component
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MyCustomAnnotation {
}
```

---

## 五、与 Spring Framework 的对比

| 特性 | Summer Framework | Spring Framework |
|------|-----------------|------------------|
| IoC 容器 | ✅ 支持注解和 XML | ✅ 支持注解、XML、Java Config |
| 依赖注入 | ✅ 构造器、字段、Setter | ✅ 同左 + 方法参数注入 |
| AOP | ✅ JDK 动态代理 | ✅ JDK 动态代理 + CGLIB |
| 事务管理 | ✅ 声明式事务 | ✅ 声明式 + 编程式事务 |
| Web MVC | ✅ 基础功能 | ✅ 完整的 MVC 框架 |
| 切点表达式 | 简单字符串匹配 | AspectJ 表达式 |
| Bean 作用域 | 仅单例 | 单例、原型、请求、会话等 |
| 循环依赖 | 构造器注入检测并抛异常 | 三级缓存解决 Setter 注入循环依赖 |

---

## 六、项目启动方式

### 1. 启动 IoC 容器（测试）

```java
public class IocTest01 {
    public static void main(String[] args) {
        // 启动容器
        AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(AppConfig.class);

        // 获取 Bean
        IUserService userService = context.getBeanByType(IUserService.class);
        userService.sayHello();
    }
}
```

### 2. 启动 Web 应用

```java
public class Application {
    public static void main(String[] args) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        tomcat.getConnector();

        Context context = tomcat.addContext("", null);
        Tomcat.addServlet(context, "dispatcher", new DispatcherServlet());
        context.addServletMappingDecoded("/", "dispatcher");

        tomcat.start();
        tomcat.getServer().await();
    }
}
```

访问：`http://localhost:8080/user/list`

---

## 七、注意事项

1. **AOP 代理类型转换**：AOP 代理返回的是接口代理，不能强转为实现类，应使用接口类型接收
2. **循环依赖**：构造器注入的循环依赖会抛异常，建议使用字段注入
3. **事务传播**：当前实现不支持事务传播行为，所有事务都是独立的
4. **线程安全**：Bean 创建过程使用了 `synchronized` 保证线程安全
5. **资源释放**：数据库连接在事务提交/回滚后自动关闭

---

## 八、未来扩展方向

1. 支持 CGLIB 代理（无需接口）
2. 支持 Bean 的多种作用域（原型、请求、会话）
3. 支持 AspectJ 切点表达式
4. 支持事务传播行为
5. 支持 POST 请求参数绑定
6. 支持 RESTful 风格的 URL（PathVariable）
7. 支持统一异常处理
8. 支持拦截器（Interceptor）
9. 支持文件上传
10. 支持国际化（i18n）

---

**文档版本**：v1.0
**最后更新**：2026-03-16
