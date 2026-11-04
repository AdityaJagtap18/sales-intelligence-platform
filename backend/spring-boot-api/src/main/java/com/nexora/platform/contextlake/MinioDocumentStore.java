package com.nexora.platform.contextlake;

import java.io.ByteArrayInputStream;

import org.springframework.stereotype.Component;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;

@Component
public class MinioDocumentStore {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioDocumentStore(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public void upload(String objectKey, byte[] content, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to upload " + objectKey + " to MinIO", e);
        }
    }
}
