package com.nexora.platform.graph;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GraphProperties.class)
public class GraphConfig {

    @Bean(destroyMethod = "close")
    public Driver neo4jDriver(GraphProperties properties) {
        return GraphDatabase.driver(properties.uri(), AuthTokens.basic(properties.user(), properties.password()));
    }
}
