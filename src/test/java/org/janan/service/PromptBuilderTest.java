package org.janan.service;


import org.janan.model.Chunk;
import org.janan.model.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

public class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

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
}
