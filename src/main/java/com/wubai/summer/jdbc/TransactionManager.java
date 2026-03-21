package com.wubai.summer.jdbc;


import com.wubai.summer.annotation.Autowired;
import com.wubai.summer.annotation.Component;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 基于 ThreadLocal 实现的简易【事务管理器】：管理事务的开启、提交、回滚
 */
@Component
public class TransactionManager {
    @Autowired
    private DataSourceConfig dataSourceConfig;  //// 1. 依赖注入数据源配置（获取数据库连接池）

        //线程内部只有一个ThreadLocalMap的局部变量，因此每个线程的内部的ThreadLocalMap都是独立的 ；
        // 而ThreadLocalMap 不会在 Thread 创建时初始化（Thread 的 threadLocals 默认为 null），而是在当前线程第一次调用任意 ThreadLocal 实例的 set() 或 get() 方法时，延迟初始化的。
            //那初始化完了之后，创建了其他ThreadLocal来set/get，都会先找到这个唯一的ThreadLocalMap，然后来直接用

    // ThreadLocal 存储当前线程的连接（保证同一事务使用同一连接）
    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>(); //所有线程共享这个ThreadLocal，是拿他作为钥匙，来开线程本身的内部箱子的


    /**
     * 获取当前线程的连接（如果没有则创建新连接）
     */
        /* 每个线程Thread都包含一个ThreadLocalMap类型的名为ThreadLocals的变量（线程私有，默认为null），本质上是一个定制的hashMap，其内部每一个entry的key值为ThreadLocal类型的实例（弱引用）；Value值为Object类型。
                存值时，调用ThreadLocal实例的set方法，如userThreadLocal.set（.....），会获取当前线程，再获取其ThreadLocalMap，然后以实例自身作为key，要保存的值的副本作为value存入里面；
                取值时，调用ThreadLocal实例的get方法，如调用userThreadLocal.get（），会先获取当前的Thread对象，再获取其内部的ThreadLocalMap对象，然后根据当前ThreadLocal实例作为key获取到对应的value值。
       */
        //数据库事务的所有操作（开启 / 提交 / 回滚）都是基于同一个数据库连接的：
    public Connection getConnection() throws SQLException {
        Connection conn = connectionHolder.get(); // → 找到当前线程 → 拿到线程内的 ThreadLocalMap → 以当前 ThreadLocal 为 key 取值（，拿🔑开自己箱子，看看箱子里有没有连接）
        if (conn == null) {
            conn = dataSourceConfig.getDataSource().getConnection();
            connectionHolder.set(conn); //以自身作为key，conn作为value 存入map
        }
        return conn;
    }





    /**
     * 开启事务
     */
    public void beginTransaction() throws SQLException {
        Connection conn = getConnection();
        conn.setAutoCommit(false);  //关掉自动提交
        System.out.println("【事务管理】开启事务");
    }


    //重要：
        //getConnection () 是 “找连接”（可能新建），
        // connectionHolder.get () 是 “拿已有的连接”（确保是事务的那个）。


    /**
     * 提交事务
     */
    // 提交事务：commit() → getConnection() → 发现 ThreadLocal 里有 conn1，直接返回（看似没问题？）；
    //   → 但如果有人不小心在 commit 前调用了 connectionHolder.remove()，getConnection() 会重新从连接池拿 conn2；
    public void commit() throws SQLException {
        Connection conn = connectionHolder.get();  //为什么 commit () 必须用 connectionHolder.get()？而不能getConnection()？
        if (conn != null) {
            conn.commit();
            System.out.println("【事务管理】提交事务成功");
        }
    }

    /**
     * 回滚事务
     */
    public void rollback() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.rollback();
                System.out.println("【事务管理】回滚事务");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭连接并清理 ThreadLocal
     */
    public void closeConnection() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                connectionHolder.remove();
            }
        }
    }
}
