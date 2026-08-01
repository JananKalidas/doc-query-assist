package org.janan.client;

import org.janan.client.dto.AnthropicRequest;
import org.janan.client.dto.AnthropicResponse;
import org.janan.exception.GenerationException;
import org.janan.service.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class AnthropicClient {

    private static final int MAX_TOKENS = 1024;
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final String model;

    public AnthropicClient(
            @Value("${anthropic.api-key}") String apiKey,
            @Value("${anthropic.base-url}") String baseUrl,
            @Value("${anthropic.model}") String model
    ){
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String generate(Prompt prompt){
        AnthropicRequest request = new AnthropicRequest(
                model,
                MAX_TOKENS,
                prompt.systemPrompt(),
                List.of(new AnthropicRequest.AnthropicMessage("user", prompt.userPrompt()))
        );

        AnthropicResponse response;
        try {
            response = restClient.post()
                    .uri("/messages")
                    .body(request)
                    .retrieve()
                    .body(AnthropicResponse.class);
        } catch (RestClientException e) {
            throw new GenerationException("Failed to generate answer via Anthropic API", e);
        }

        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new GenerationException("Anthropic API returned an empty response");
        }

        return response.content().get(0).text();
    }

}
