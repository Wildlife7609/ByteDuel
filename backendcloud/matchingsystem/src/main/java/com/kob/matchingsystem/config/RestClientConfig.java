package com.kob.matchingsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// baseUrl 填的是要调用的服务的地址：matchingsystem 调用 backend
@Configuration
public class RestClientConfig {
    @Bean
    public RestClient backendClient() {
        return RestClient.builder().baseUrl("http://127.0.0.1:8080").build();
    }
}
