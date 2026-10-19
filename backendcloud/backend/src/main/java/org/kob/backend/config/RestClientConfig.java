package org.kob.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// baseUrl is the address of the service being called; backend calls two services, so there are two RestClients
// every request carries the internal shared secret
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
