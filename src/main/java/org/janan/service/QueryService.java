package org.janan.service;

import org.janan.client.GenerationClient;
import org.janan.dto.AskResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryService {

    private final RetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final GenerationClient generationClient;

    public QueryService(
            RetrievalService retrievalService,
            PromptBuilder promptBuilder,
            GenerationClient generationClient) {
        this.retrievalService = retrievalService;
        this.promptBuilder = promptBuilder;
        this.generationClient = generationClient;
    }

    public AskResponse ask(String question) {
        // Throws NoRelevantChunkFoundException (-> 422) if nothing clears
        // the similarity threshold - propagates up to the global handler.
        List<RetrievedChunk> retrieved = retrievalService.retrieve(question);

        Prompt prompt = promptBuilder.build(question, retrieved);
        String answer = generationClient.generate(prompt);

        return AskResponse.of(question, answer, retrieved);
    }
}
