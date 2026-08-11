package org.janan.service;

import org.janan.client.GenerationClient;
import org.janan.dto.AskResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryService {

    private final QueryValidator queryValidator;
    private final RetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final GenerationClient generationClient;

    public QueryService(
            QueryValidator queryValidator,
            RetrievalService retrievalService,
            PromptBuilder promptBuilder,
            GenerationClient generationClient) {
        this.queryValidator = queryValidator;
        this.retrievalService = retrievalService;
        this.promptBuilder = promptBuilder;
        this.generationClient = generationClient;
    }

    public AskResponse ask(String question) {

        queryValidator.validate(question);
        // Throws NoRelevantChunkFoundException (-> 422) if nothing clears
        List<RetrievedChunk> retrieved = retrievalService.retrieve(question);

        Prompt prompt = promptBuilder.build(question, retrieved);
        String answer = generationClient.generate(prompt);

        return AskResponse.of(question, answer, retrieved);
    }
}
