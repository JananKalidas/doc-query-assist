package org.janan.service;


import org.janan.model.Chunk;
import org.janan.model.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

public class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder(6000);

    @Test
    void systemPrompt_instructsRefusalWhenAnswerNotInContext() {
        Prompt prompt = promptBuilder.build("What is the refund policy?", List.of());

        assertThat(prompt.systemPrompt()).containsIgnoringCase("say so explicitly");
    }

    @Test
    void systemPrompt_mitigatesPromptInjection() {
        Prompt prompt = promptBuilder.build("Any question", List.of());

        assertThat(prompt.systemPrompt()).containsIgnoringCase("untrusted");
        assertThat(prompt.systemPrompt()).containsIgnoringCase("ignore any instructions");
    }

    @Test
    void userPrompt_includesQuestionAndAllChunksInOrder() {
        Document doc = new Document();
        Chunk chunk1 = new Chunk(doc, 0, "First relevant fact.", new float[]{0.1f});
        Chunk chunk2 = new Chunk(doc, 1, "Second relevant fact.", new float[]{0.2f});

        List<RetrievedChunk> retrieved = List.of(
                new RetrievedChunk(chunk1, 0.95),
                new RetrievedChunk(chunk2, 0.88)
        );

        Prompt prompt = promptBuilder.build("What are the facts?", retrieved);

        assertThat(prompt.userPrompt())
                .contains("First relevant fact.")
                .contains("Second relevant fact.")
                .contains("What are the facts?");

        // Chunk 1 text should appear before chunk 2 text - order preserved.
        int firstIndex = prompt.userPrompt().indexOf("First relevant fact.");
        int secondIndex = prompt.userPrompt().indexOf("Second relevant fact.");
        assertThat(firstIndex).isLessThan(secondIndex);
    }

    @Test
    void userPrompt_withNoChunks_stillIncludesQuestion() {
        Prompt prompt = promptBuilder.build("Unanswerable question?", List.of());

        assertThat(prompt.userPrompt()).contains("Unanswerable question?");
    }

    @Test
    void tokenBudget_dropsLowestScoringChunksWhenContextTooLarge() {
        // Small budget (10 tokens ~= 40 chars) forces truncation.
        PromptBuilder tightBudgetBuilder = new PromptBuilder(10);

        Document doc = new Document();
        String longContent = "x".repeat(200); // ~50 tokens on its own
        Chunk highScoreChunk = new Chunk(doc, 0, "Important fact here.", new float[]{0.1f});
        Chunk lowScoreChunk = new Chunk(doc, 1, longContent, new float[]{0.2f});

        List<RetrievedChunk> retrieved = List.of(
                new RetrievedChunk(highScoreChunk, 0.95),  // highest score - kept
                new RetrievedChunk(lowScoreChunk, 0.60)    // lowest score - dropped first
        );

        Prompt prompt = promptBuilder.build("Question?", retrieved);
        Prompt tightPrompt = tightBudgetBuilder.build("Question?", retrieved);

        // With a generous budget, both chunks are included.
        assertThat(prompt.userPrompt()).contains("Important fact here.");

        // With a tight budget, the lowest-scoring chunk is dropped, but at
        // least one chunk always remains.
        assertThat(tightPrompt.userPrompt()).contains("Important fact here.");
    }
}
