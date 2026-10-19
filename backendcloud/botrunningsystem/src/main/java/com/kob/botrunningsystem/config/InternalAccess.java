package com.kob.botrunningsystem.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// Access control for internal endpoints (called only between services); backend, matchingsystem and botrunningsystem each have an identical copy
// The caller sends the shared secret in the X-Internal-Token header, and the callee checks it
// Without a configured secret (local dev), only localhost may call. Under Docker the services are not on the same host, so the secret is required
@Component
public class InternalAccess {
    public static final String HEADER = "X-Internal-Token";
    private static final IpAddressMatcher LOCALHOST = new IpAddressMatcher("127.0.0.1");

    private final String token;

    public InternalAccess(@Value("${internal.token}") String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public boolean isAllowed(HttpServletRequest request) {
        if (token.isBlank())
            return LOCALHOST.matches(request);
        String header = request.getHeader(HEADER);
        // constant-time comparison, so the secret cannot be guessed byte by byte from response times
        return header != null && MessageDigest.isEqual(
                header.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }
}
