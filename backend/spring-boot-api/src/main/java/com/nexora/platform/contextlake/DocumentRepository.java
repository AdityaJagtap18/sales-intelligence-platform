package com.nexora.platform.contextlake;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findAllByOrderByUploadedAtDesc();
}
