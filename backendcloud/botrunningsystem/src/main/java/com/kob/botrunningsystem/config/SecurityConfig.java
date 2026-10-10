package com.kob.botrunningsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
        private final InternalAccess internalAccess;

        public SecurityConfig(InternalAccess internalAccess) {
                this.internalAccess = internalAccess;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // 只给 backend 调用
                                                .requestMatchers("/bot/add/", "/bot/compile/")
                                                .access((authentication, context) -> new AuthorizationDecision(
                                                                internalAccess.isAllowed(context.getRequest())))
                                                .anyRequest().denyAll());
                return http.build();
        }
}
