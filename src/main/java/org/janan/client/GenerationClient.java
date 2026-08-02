package org.janan.client;

import org.janan.client.dto.GeminiGenerationRequest;
import org.janan.client.dto.GeminiGenerationResponse;
import org.janan.exception.GenerationException;
import org.janan.service.Prompt;
import org.janan.client.dto.GeminiGenerationRequest.Content;
import org.janan.client.dto.GeminiGenerationRequest.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class GenerationClient {
    private static final Logger log = LoggerFactory.getLogger(GenerationClient.class);

    private final RestClient restClient;
    private final String model;

    public GenerationClient(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.base-url}") String baseUrl,
            @Value("${gemini.generation-model}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String generate(Prompt prompt) {
        GeminiGenerationRequest request = new GeminiGenerationRequest(
                List.of(new Content("user", List.of(new Part(prompt.userPrompt())))),
                new GeminiGenerationRequest.SystemInstruction(List.of(new Part(prompt.systemPrompt())))
        );

        GeminiGenerationResponse response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .body(request)
                    .retrieve()
                    .body(GeminiGenerationResponse.class);
        } catch (RestClientException e) {
            log.error("Gemini generation call failed: {}", e.getMessage(), e);
            throw new GenerationException("Failed to generate answer via Gemini API", e);
        }

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new GenerationException("Gemini API returned an empty response");
        }

        List<GeminiGenerationResponse.Part> parts = response.candidates().get(0).content().parts();
        if (parts == null || parts.isEmpty()) {
            throw new GenerationException("Gemini API returned a response with no content parts");
        }

        return parts.get(0).text();
    }
}
