package com.kob.botrunningsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

// baseUrl is the address of the service being called: botrunningsystem calls backend and the go-judge sandbox
@Configuration
public class RestClientConfig {
    @Bean
    public RestClient backendClient(@Value("${services.backend-url}") String url, InternalAccess internalAccess) {
        return RestClient.builder().baseUrl(url)
                .defaultHeader(InternalAccess.HEADER, internalAccess.getToken()).build(); // carries the internal shared secret
    }

    @Bean
    public RestClient sandboxClient(@Value("${sandbox.url}") String sandboxUrl) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(90)); // compiling takes up to 60 seconds; leave some margin
        return RestClient.builder().baseUrl(sandboxUrl).requestFactory(requestFactory).build();
    }
}
