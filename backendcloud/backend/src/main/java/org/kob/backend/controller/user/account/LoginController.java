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

    // Too many failed logins and the account is temporarily locked (see LoginAttemptLimiter): return 429 Too Many Requests; the frontend shows error_message
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, String>> handleLocked(LockedException e) {
        long minutes = LoginAttemptLimiter.LOCK_DURATION.toMinutes();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error_message", "Too many failed login attempts. This account is locked for " + minutes + " minutes. Please try again later."));
    }
}
