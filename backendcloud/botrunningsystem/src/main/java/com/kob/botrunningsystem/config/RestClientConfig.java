package com.kob.botrunningsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

// baseUrl 填的是要调用的服务的地址：botrunningsystem 调用 backend 和 go-judge 沙箱
@Configuration
public class RestClientConfig {
    @Bean
    public RestClient backendClient(@Value("${services.backend-url}") String url, InternalAccess internalAccess) {
        return RestClient.builder().baseUrl(url)
                .defaultHeader(InternalAccess.HEADER, internalAccess.getToken()).build(); // 带上内部接口的共享密钥
    }

    @Bean
    public RestClient sandboxClient(@Value("${sandbox.url}") String sandboxUrl) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(90)); // 编译最长 60 秒，留点余量
        return RestClient.builder().baseUrl(sandboxUrl).requestFactory(requestFactory).build();
    }
}
