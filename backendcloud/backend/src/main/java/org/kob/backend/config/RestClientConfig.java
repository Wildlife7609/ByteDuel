package org.kob.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// baseUrl 填的是要调用的服务的地址；backend 要调用两个服务，所以有两个 RestClient
// 请求都带上内部接口的共享密钥
@Configuration
public class RestClientConfig {
    @Bean
    public RestClient matchingClient(@Value("${services.matching-url}") String url, InternalAccess internalAccess) {
        return RestClient.builder().baseUrl(url)
                .defaultHeader(InternalAccess.HEADER, internalAccess.getToken()).build();
    }

    @Bean
    public RestClient botRunningClient(@Value("${services.bot-running-url}") String url, InternalAccess internalAccess) {
        return RestClient.builder().baseUrl(url)
                .defaultHeader(InternalAccess.HEADER, internalAccess.getToken()).build();
    }
}
