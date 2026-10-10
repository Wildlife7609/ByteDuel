package org.kob.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// 内部接口（只给服务之间调用）的访问控制，backend、matchingsystem、botrunningsystem 各有一份相同的
// 调用方在请求头 X-Internal-Token 里带上共享密钥，被调用方核对
// 没配置密钥时（本地开发）退回到只允许本机调用；部署到 Docker 后各服务不在同一个“本机”，必须配置密钥
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
        // 用固定时间的比较，避免通过响应时间逐位猜出密钥
        return header != null && MessageDigest.isEqual(
                header.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }
}
