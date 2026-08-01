package org.janan.service;

import org.janan.client.AnthropicClient;
import org.janan.dto.AskResponse;

import java.util.List;

public class QueryService {

    private final RetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final AnthropicClient anthropicClient;

    public QueryService(
            RetrievalService retrievalService,
            PromptBuilder promptBuilder,
            AnthropicClient anthropicClient) {
        this.retrievalService = retrievalService;
        this.promptBuilder = promptBuilder;
        this.anthropicClient = anthropicClient;
    }

    public AskResponse ask(String question) {
        List<RetrievedChunk> retrieved = retrievalService.retrieve(question);

        Prompt prompt = promptBuilder.build(question, retrieved);
        String answer = anthropicClient.generate(prompt);

        return AskResponse.of(question, answer, retrieved);
    }
}
