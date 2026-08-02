package org.janan.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiEmbeddingResponse(List<EmbeddingValues> embeddings) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmbeddingValues(List<Float> values) {
    }
}
