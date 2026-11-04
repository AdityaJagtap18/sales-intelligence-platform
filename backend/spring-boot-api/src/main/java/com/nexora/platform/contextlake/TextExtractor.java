package com.nexora.platform.contextlake;

import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/** Supports the document types the Context Lake accepts: plain text, markdown, and PDF. */
@Component
public class TextExtractor {

    public String extract(String contentType, byte[] content) {
        if ("application/pdf".equals(contentType)) {
            return extractPdf(content);
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private String extractPdf(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            return new PDFTextStripper().getText(document);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to extract text from PDF", e);
        }
    }
}
