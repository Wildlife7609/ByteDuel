package org.kob.backend.controller.user.account;

import org.kob.backend.service.impl.utils.LoginAttemptLimiter;
import org.kob.backend.service.user.account.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class LoginController {
    @Autowired
    private LoginService loginService;

    @PostMapping("/user/account/token/")
    public Map<String, String> getToken(@RequestBody Map<String, String> map) {
        String username = map.get("username");
        String password = map.get("password");
        return loginService.getToken(username, password);
    }

    // 登录失败次数过多、账号被暂时锁定（见 LoginAttemptLimiter）：返回 429（请求过多），前端显示 error_message
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, String>> handleLocked(LockedException e) {
        long minutes = LoginAttemptLimiter.LOCK_DURATION.toMinutes();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error_message", "登录失败次数过多，账号已锁定 " + minutes + " 分钟，请稍后再试"));
    }
}
