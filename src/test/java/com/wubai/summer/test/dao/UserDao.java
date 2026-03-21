package com.wubai.summer.test.dao;

import com.wubai.summer.annotation.Autowired;
import com.wubai.summer.annotation.Component;
import com.wubai.summer.jdbc.JdbcTemplate;
import com.wubai.summer.test.pojo.User;

import java.sql.SQLException;
import java.util.List;

@Component
public class UserDao {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void insert(User user) throws SQLException {
        String sql = "INSERT INTO user (name, age) VALUES (?, ?)";
        jdbcTemplate.update(sql, user.getName(), user.getAge());
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM user";
        return jdbcTemplate.query(sql, rs -> {
            User user = new User();
            user.setName(rs.getString("name"));
            user.setAge(rs.getInt("age"));
            return user;
        });
    }
}