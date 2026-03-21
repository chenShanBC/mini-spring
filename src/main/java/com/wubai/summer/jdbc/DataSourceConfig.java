package com.wubai.summer.jdbc;

import com.alibaba.druid.pool.DruidDataSource;
import com.wubai.summer.annotation.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * 数据源配置（使用 Druid 连接池）
 */
@Component  //被ioc管理，那么这个DataSourceConfig就是个单例
public class DataSourceConfig {
    private final DataSource dataSource;

    public DataSourceConfig() {
            //这个DataSourceConfig是bean，由ioc来管理，
            // 而其他线程需要的话只需要从ioc里拿就行，不必调用其构造；
                // 因此该构造器只会由ioc反射调用，且只会调用一次 ， 从而保证该数据源DataSource dataSource 也必然是唯一的单例，线程天然安全
        DruidDataSource druid = new DruidDataSource();

        // 基本配置
        druid.setUrl("jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        druid.setUsername("root");
        druid.setPassword("admin");  // 修改为你的 MySQL 密码
        druid.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // 连接池配置
        druid.setInitialSize(5);        // 初始连接数
        druid.setMinIdle(5);            // 最小空闲连接
        druid.setMaxActive(20);         // 最大活跃连接
        druid.setMaxWait(60000);        // 获取连接最大等待时间（毫秒）

        // 监控配置（Druid 特色功能）
        try {
            druid.setFilters("stat,wall");  // 开启监控统计和SQL防火墙
        } catch (SQLException e) {
            e.printStackTrace();
        }

        this.dataSource = druid;
        System.out.println("【数据源】Druid连接池初始化完成");
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}