package com.wubai.summer.test.Services;

import com.wubai.summer.annotation.Autowired;
import com.wubai.summer.annotation.Component;
import com.wubai.summer.test.pojo.User;


@Component
public class OrderService implements IOrderService {
    private IUserService userService;  // 改为接口类型
    private User user;

    // 构造器注入（带@Autowired，优先选择）
    @Autowired
    public OrderService(IUserService userService) {  // 参数改为接口类型
        this.userService = userService;
    }

    // Setter注入
    @Autowired
    public void setUser(User user) {
        this.user = user;
    }

    public void createOrder() {
        System.out.println("OrderService: 创建订单，用户：" + user + "，服务：" + userService);
    }
}