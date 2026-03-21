package com.wubai.summer.test.Services;

import com.wubai.summer.annotation.Autowired;
import com.wubai.summer.annotation.Component;
import com.wubai.summer.annotation.tx.Transactional;
import com.wubai.summer.test.dao.UserDao;
import com.wubai.summer.test.pojo.User;

import java.sql.SQLException;



@Component
public class UserService implements IUserService {
    @Autowired
    private User user;

    public void setUser(User user) { this.user = user; }
    public User getUser() { return user; }
    public void sayHello() { System.out.println("UserService: Hello " + user); }

    @Autowired
    private UserDao userDao;

    @Transactional
    public void saveUser(User user) throws SQLException {
        userDao.insert(user);
        System.out.println("保存用户：" + user);
    }

    @Transactional
    public void saveUserWithError(User user) throws SQLException {
        userDao.insert(user);
        System.out.println("保存用户：" + user);

        // 模拟异常，触发回滚
        throw new RuntimeException("模拟异常，触发事务回滚");
    }
}
