package com.nexora.platform.contextlake;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document")
public class Document {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_INDEXED = "INDEXED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private String status;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected Document() {
    }

    public Document(String filename, String objectKey, String contentType, long sizeBytes, Instant uploadedAt) {
        this.filename = filename;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.status = STATUS_PENDING;
        this.chunkCount = 0;
        this.uploadedAt = uploadedAt;
    }

    public void markIndexed(int chunkCount) {
        this.status = STATUS_INDEXED;
        this.chunkCount = chunkCount;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = STATUS_FAILED;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getStatus() {
        return status;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
