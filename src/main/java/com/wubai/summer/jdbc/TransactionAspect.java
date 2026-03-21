package com.wubai.summer.jdbc;

import com.wubai.summer.annotation.Autowired;
import com.wubai.summer.annotation.Component;
import com.wubai.summer.annotation.aop.Around;
import com.wubai.summer.annotation.aop.Aspect;
import com.wubai.summer.core.aop.ProceedingJoinPoint;

/**
 * 事务切面：拦截 @Transactional 方法
 */
@Aspect
@Component
public class TransactionAspect {
    @Autowired
    private TransactionManager transactionManager;

    @Around("*Service")
    public Object handleTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        // 检查方法是否有 @Transactional 注解
        if (!joinPoint.getMethod().isAnnotationPresent(
                com.wubai.summer.annotation.tx.Transactional.class)) {
            // 没有注解，直接执行
            return joinPoint.proceed();
        }

        // 开启事务（获取连接，关闭自动提交）
        transactionManager.beginTransaction();

        try {
            // 执行目标方法
            Object result = joinPoint.proceed();

            // 提交事务
            transactionManager.commit();
            return result;

        } catch (Throwable e) {
            // 回滚事务（内部调用conn.rollback();数据库级回滚）
            transactionManager.rollback();
            throw e;

        } finally {
            // 关闭连接
            transactionManager.closeConnection();
        }
    }

}
