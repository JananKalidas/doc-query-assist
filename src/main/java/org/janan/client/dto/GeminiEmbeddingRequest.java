package org.janan.client.dto;

import java.util.List;

public record GeminiEmbeddingRequest(
        List<GeminiEmbedRequestItem> requests
) {
    public record GeminiEmbedRequestItem(
            String model,
            Content content,
            String taskType,
            int outputDimensionality
    ) {
    }

    public record Content(List<Part> parts) {
    }

    public record Part(String text) {
    }
}
