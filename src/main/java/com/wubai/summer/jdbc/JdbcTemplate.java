package com.wubai.summer.jdbc;



import com.wubai.summer.annotation.Autowired;
import com.wubai.summer.annotation.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JdbcTemplate：简化 JDBC 操作
 */
@Component
public class JdbcTemplate {
    @Autowired
    private TransactionManager transactionManager;

    /**
     * 执行更新操作（INSERT、UPDATE、DELETE）
     */
    public int update(String sql, Object... params) throws SQLException {
        // 1. 获取数据库连接（来自事务管理器，保证事务一致性）
        Connection conn = transactionManager.getConnection();
        // 2. 创建 PreparedStatement 对象（德鲁伊的连接会提供）,用来执行sql的对象（核心步骤，能防止sql注入）
        // try-with-resources 语法：自动关闭资源，不用手动写 ps.close()
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // 3. 设置参数：把 params 里的值填充到 SQL 的占位符（?）中
            setParameters(ps, params);
            // 4. 执行更新操作，返回受影响的行数
            return ps.executeUpdate();
        }
    }

    //update的一个执行例子：
    // 1. 定义带占位符的SQL
    //String sql = "UPDATE user SET age = ? WHERE id = ?";
    // 2. 调用 update 方法，传入SQL和参数
    //int affectedRows = jdbcTemplate.update(sql, 25, 1001);
    // 3. 结果：affectedRows 是 1（表示id=1001的用户被修改）




    /**
     * 查询单个对象
     */
    //解耦：把「执行 SQL」和「封装对象」的逻辑分开，查询方法更通用，封装逻辑更灵活
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... params) throws SQLException {
        List<T> list = query(sql, rowMapper, params);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 查询列表
     */
    //解耦：把「执行 SQL」和「封装对象」的逻辑分开，查询方法更通用，封装逻辑更灵活
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... params) throws SQLException {
        Connection conn = transactionManager.getConnection();
        List<T> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) { // ResultSet 想象成一个「带游标（指针）的表格」：
                // 初始时，游标指向「第一行数据之前」；
                // 调用 rs.next() 时，游标向下移动一行：
                    // 有数据，返回 true，可以用 getXxx() 取字段值
                    // 没有数据，返回 false，结束遍历。
                while (rs.next()) {
                    result.add(rowMapper.mapRow(rs));
                }
            }
        }
        return result;
    }

    /**
     * 设置 SQL 参数
     */
    private void setParameters(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    /**
     * 行映射器接口
     */
    //关于这个接口和查询的调用逻辑
        //封装 RowMapper（定义对象映射规则）→ 调用 JdbcTemplate 查询方法（传入 RowMapper）→ 通过 Druid 连接获取 PreparedStatement → 执行 executeQuery 得到 ResultSet → 用 next() 遍历行 → 调用 mapRow 把每行 ResultSet 转成对象
    @FunctionalInterface    //函数式接口  这个接口只有一个抽象方法
    public interface RowMapper<T> { //定义「把数据库查询结果集（ResultSet）中的一行数据，转换成你想要的 Java 对象（比如 User、Order）」的规则
        T mapRow(ResultSet rs) throws SQLException;
    }
}