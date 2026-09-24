package org.kob.backend.config.filter;

import org.kob.backend.mapper.UserMapper;
import org.kob.backend.pojo.User;
import org.kob.backend.service.impl.utils.UserDetailsImpl;
import org.kob.backend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    @Autowired
    private UserMapper userMapper;

    @Override
    protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
            throws jakarta.servlet.ServletException, IOException {
        String token = request.getHeader("Authorization");

        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        token = token.substring(7);
        Integer userId = JwtUtil.getUserId(token);
        if (userId == -1) {
            filterChain.doFilter(request, response);
            return;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        UserDetailsImpl loginUser = new UserDetailsImpl(user);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser,
                null, null);

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        filterChain.doFilter(request, response);
    }
}