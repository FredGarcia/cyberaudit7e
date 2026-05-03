package com.cyberaudit7e.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration RestTemplate pour les appels HTTP sortants
 * vers ServiceNow et SailPoint.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000); // 10 secondes
        factory.setReadTimeout(30_000); // 30 secondes
        return new RestTemplate(factory);
    }
}