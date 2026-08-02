package org.janan.client;

import org.janan.client.dto.GeminiEmbeddingRequest;
import org.janan.client.dto.GeminiEmbeddingResponse;
import org.janan.exception.EmbeddingGenerationException;
import org.janan.client.dto.GeminiEmbeddingRequest.Content;
import org.janan.client.dto.GeminiEmbeddingRequest.Part;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);
    private static final int EMBEDDING_DIMENSIONS = 1536;

    private final RestClient restClient;
    private final String model;

    public EmbeddingClient(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.base-url}") String baseUrl,
            @Value("${gemini.embedding-model}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Embeds a batch of document chunks in a single API call. Tagged as
     * RETRIEVAL_DOCUMENT - Gemini's embeddings are asymmetric, so document
     * chunks and queries are embedded slightly differently for better
     * retrieval quality.
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        return callBatchEmbed(texts, "RETRIEVAL_DOCUMENT");
    }

    /**
     * Embeds a single incoming user query. Tagged as RETRIEVAL_QUERY.
     */
    public float[] embed(String text) {
        List<float[]> result = callBatchEmbed(List.of(text), "RETRIEVAL_QUERY");
        return result.get(0);
    }

    private List<float[]> callBatchEmbed(List<String> texts, String taskType) {
        List<GeminiEmbeddingRequest.GeminiEmbedRequestItem> items = texts.stream()
                .map(text -> new GeminiEmbeddingRequest.GeminiEmbedRequestItem(
                        "models/" + model,
                        new Content(List.of(new Part(text))),
                        taskType,
                        EMBEDDING_DIMENSIONS))
                .toList();

        GeminiEmbeddingRequest request = new GeminiEmbeddingRequest(items);

        GeminiEmbeddingResponse response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:batchEmbedContents", model)
                    .body(request)
                    .retrieve()
                    .body(GeminiEmbeddingResponse.class);
        } catch (RestClientException e) {
            log.error("Gemini embeddings call failed: {}", e.getMessage(), e);
            throw new EmbeddingGenerationException(
                    "Failed to generate embeddings via Gemini API", e);
        }

        if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
            throw new EmbeddingGenerationException(
                    "Gemini embeddings API returned an empty response");
        }

        return response.embeddings().stream()
                .map(this::toFloatArray)
                .toList();
    }

    private float[] toFloatArray(GeminiEmbeddingResponse.EmbeddingValues data) {
        List<Float> values = data.values();
        float[] array = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }
}
