package com.bank.report.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** WebClient configuration; the URL comes from the config server. */
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient transactionWebClient(WebClient.Builder builder,
                                          @Value("${services.transaction.url}") String baseUrl) {
        return builder.clone().baseUrl(baseUrl).build();
    }
}