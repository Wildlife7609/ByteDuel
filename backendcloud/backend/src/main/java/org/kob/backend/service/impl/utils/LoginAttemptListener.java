package org.kob.backend.service.impl.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

// 监听 Spring Security 的登录事件，更新登录失败次数（见 LoginAttemptLimiter）
// 用户名不存在也算密码错误（Spring Security 默认不区分这两种情况），所以一样会被计数
@Component
public class LoginAttemptListener {
    @Autowired
    private LoginAttemptLimiter loginAttemptLimiter;

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        loginAttemptLimiter.recordFailure(event.getAuthentication().getName());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        loginAttemptLimiter.recordSuccess(event.getAuthentication().getName());
    }
}
