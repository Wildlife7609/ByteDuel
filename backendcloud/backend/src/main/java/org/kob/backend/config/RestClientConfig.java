package org.kob.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// baseUrl 填的是要调用的服务的地址；backend 要调用两个服务，所以有两个 RestClient
@Configuration
public class RestClientConfig {
    @Bean
    public RestClient matchingClient() {
        return RestClient.builder().baseUrl("http://127.0.0.1:8081").build();
    }

    @Bean
    public RestClient botRunningClient() {
        return RestClient.builder().baseUrl("http://127.0.0.1:8082").build();
    }
}
