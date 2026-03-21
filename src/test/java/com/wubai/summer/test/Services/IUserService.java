package com.wubai.summer.test.Services;

import com.wubai.summer.annotation.tx.Transactional;
import com.wubai.summer.test.pojo.User;

import java.sql.SQLException;

/**
 * UserService 接口
 */
public interface IUserService {
    void sayHello();

    @Transactional
    void saveUser(User user1) throws SQLException ;

    @Transactional
    void saveUserWithError(User user2) throws SQLException;
}
