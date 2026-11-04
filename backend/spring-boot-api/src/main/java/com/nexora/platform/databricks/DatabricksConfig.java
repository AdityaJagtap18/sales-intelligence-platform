package com.nexora.platform.databricks;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(DatabricksProperties.class)
public class DatabricksConfig {

    @Bean
    public RestClient databricksRestClient(DatabricksProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.host())
                .defaultHeader("Authorization", "Bearer " + properties.token())
                .build();
    }
}
