package com.nexora.platform.contextlake;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import io.minio.MinioClient;

@Configuration
@EnableConfigurationProperties({ MinioProperties.class, QdrantProperties.class, OllamaProperties.class })
public class ContextLakeConfig {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @Bean
    public RestClient qdrantRestClient(QdrantProperties properties) {
        return RestClient.builder().baseUrl(properties.url()).build();
    }

    @Bean
    public RestClient ollamaRestClient(OllamaProperties properties) {
        return RestClient.builder().baseUrl(properties.url()).build();
    }
}
