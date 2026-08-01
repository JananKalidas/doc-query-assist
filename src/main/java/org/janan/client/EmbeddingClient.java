package org.janan.client;

import org.janan.client.dto.OpenAiEmbeddingRequest;
import org.janan.client.dto.OpenAiEmbeddingResponse;
import org.janan.exception.EmbeddingGenerationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Comparator;
import java.util.List;

@Component
public class EmbeddingClient {
    private final RestClient restClient;
    private final String model;

    public EmbeddingClient(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.embedding-model}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        OpenAiEmbeddingRequest request = new OpenAiEmbeddingRequest(model, texts);

        OpenAiEmbeddingResponse response;
        try {
            response = restClient.post()
                    .uri("/embeddings")
                    .body(request)
                    .retrieve()
                    .body(OpenAiEmbeddingResponse.class);
        } catch (RestClientException e) {
            throw new EmbeddingGenerationException(
                    "Failed to generate embeddings via OpenAI API", e);
        }

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new EmbeddingGenerationException(
                    "OpenAI embeddings API returned an empty response");
        }

        return response.data().stream()
                .sorted(Comparator.comparingInt(OpenAiEmbeddingResponse.EmbeddingData::index))
                .map(this::toFloatArray)
                .toList();
    }

    public float[] embed(String text) {
        List<float[]> result = embedBatch(List.of(text));
        return result.get(0);
    }


    private float[] toFloatArray(OpenAiEmbeddingResponse.EmbeddingData data) {
        List<Float> values = data.embedding();
        float[] array = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }
}
