package org.janan.dto;

import org.janan.model.Document;

import java.util.UUID;

public record UploadResponse (
        UUID documentId,
        String fileName,
        String status
) {
    public static UploadResponse from(Document document) {
        return new UploadResponse(
                document.getId(),
                document.getFileName(),
                document.getStatus().name()
        );
    }
}