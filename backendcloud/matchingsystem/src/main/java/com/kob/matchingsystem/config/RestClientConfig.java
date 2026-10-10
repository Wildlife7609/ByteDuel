package com.kob.matchingsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// baseUrl 填的是要调用的服务的地址：matchingsystem 调用 backend，请求带上内部接口的共享密钥
@Configuration
public class RestClientConfig {
    @Bean
    public RestClient backendClient(@Value("${services.backend-url}") String url, InternalAccess internalAccess) {
        return RestClient.builder().baseUrl(url)
                .defaultHeader(InternalAccess.HEADER, internalAccess.getToken()).build();
    }
}
