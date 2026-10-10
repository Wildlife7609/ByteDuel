package org.kob.backend.service.impl.utils;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// 登录失败次数限制，防止暴力破解密码：同一个用户名在 WINDOW 内输错 MAX_FAILURES 次，锁定 LOCK_DURATION
// 计数由 LoginAttemptListener 根据 Spring Security 的登录事件更新；锁定状态通过 UserDetailsImpl.isAccountNonLocked()
// 交给 Spring Security，被锁定时它在校验密码之前就会拒绝登录（抛出 LockedException）
// 按用户名而不是按 IP 计数：请求经过 Cloudflare 和 Caddy 转发，backend 拿到的 IP 不是用户的真实 IP
// 计数只存在内存里：backend 只有一个实例，重启后清零也没关系
@Component
public class LoginAttemptLimiter {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    public static final Duration LOCK_DURATION = Duration.ofMinutes(10);
    private static final int MAX_ENTRIES = 100_000; // 防止有人用大量随机用户名把内存撑满

    // firstFailure：这一轮计数开始的时间；lockedUntil：锁定到什么时候，没锁定是 null
    private record Attempt(int failures, Instant firstFailure, Instant lockedUntil) {
        boolean expired(Instant now) {
            if (lockedUntil != null)
                return !now.isBefore(lockedUntil);
            return !now.isBefore(firstFailure.plus(WINDOW));
        }
    }

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String username) {
        Attempt attempt = attempts.get(username);
        return attempt != null && attempt.lockedUntil() != null && Instant.now().isBefore(attempt.lockedUntil());
    }

    public void recordFailure(String username) {
        Instant now = Instant.now();
        if (attempts.size() >= MAX_ENTRIES)
            attempts.values().removeIf(attempt -> attempt.expired(now));
        attempts.compute(username, (key, attempt) -> {
            if (attempt == null || attempt.expired(now))
                attempt = new Attempt(0, now, null); // 上一轮过期了，重新计数
            int failures = attempt.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_DURATION) : null;
            return new Attempt(failures, attempt.firstFailure(), lockedUntil);
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(username);
    }
}
