package com.kob.matchingsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// baseUrl is the address of the service being called: matchingsystem calls backend, with the internal shared secret on every request
@Configuration
public class RestClientConfig {
    @Bean
    public RestClient backendClient(@Value("${services.backend-url}") String url, InternalAccess internalAccess) {
        return RestClient.builder().baseUrl(url)
                .defaultHeader(InternalAccess.HEADER, internalAccess.getToken()).build();
    }
}
