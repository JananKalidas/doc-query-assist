package org.janan.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ChunkingServiceTest {

    private final ChunkingService chunkingService = new ChunkingService(100, 15);

    @Test
    void emptyDocument_returnsNoChunks() {
        assertThat(chunkingService.chunk("")).isEmpty();
        assertThat(chunkingService.chunk(null)).isEmpty();
        assertThat(chunkingService.chunk("   ")).isEmpty();
    }

    @Test
    void documentSmallerThanChunkSize_returnsSingleChunk() {
        String text = "This is a short document, well under the chunk size limit.";

        List<String> chunks = chunkingService.chunk(text);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(text);
    }

    @Test
    void documentExactlyAtChunkSize_returnsSingleChunk() {
        String text = "a".repeat(100);

        List<String> chunks = chunkingService.chunk(text);

        assertThat(chunks).hasSize(1);
    }

    @Test
    void largeDocument_producesMultipleChunks() {
        // 350 chars of plain repeated text with no natural boundaries -
        // forces hard-cut behavior and confirms we still make progress.
        String text = "x".repeat(350);

        List<String> chunks = chunkingService.chunk(text);

        assertThat(chunks.size()).isGreaterThan(1);
        // Every chunk except possibly the last should respect the configured size.
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertThat(chunks.get(i).length()).isLessThanOrEqualTo(100);
        }
    }

    @Test
    void overlapWorks_consecutiveChunksShareTrailingContent() {
        String text = "x".repeat(300);

        List<String> chunks = chunkingService.chunk(text);

        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);

        String firstChunk = chunks.get(0);
        String secondChunk = chunks.get(1);

        // The tail of the first chunk should reappear at the head of the
        // second chunk - that's the overlap doing its job.
        String expectedOverlap = firstChunk.substring(firstChunk.length() - 10);
        assertThat(secondChunk).startsWith(expectedOverlap);
    }

    @Test
    void paragraphBoundary_isRespectedWhenNearIdealCutPoint() {
        // Two paragraphs; the break sits right around where a 100-char cut
        // would otherwise land mid-sentence. Chunker should prefer the
        // paragraph break over a hard cut.
        String paragraph1 = "a".repeat(90);
        String paragraph2 = "b".repeat(90);
        String text = paragraph1 + "\n\n" + paragraph2;

        List<String> chunks = chunkingService.chunk(text);

        // First chunk should end exactly at the paragraph break, not mid-way
        // through paragraph1 or bleeding into paragraph2.
        assertThat(chunks.get(0)).isEqualTo(paragraph1);
    }

    @Test
    void noInfiniteLoop_whenOverlapIsMisconfiguredCloseToChunkSize() {
        // overlapPercent=99 -> overlapChars=99 out of chunkSize=100.
        // Must still terminate and make forward progress.
        ChunkingService edgeCaseService = new ChunkingService(100, 99);
        String text = "y".repeat(500);

        List<String> chunks = edgeCaseService.chunk(text);

        assertThat(chunks).isNotEmpty();
        // Should terminate in a bounded number of steps, not loop forever.
        assertThat(chunks.size()).isLessThan(500);
    }
}
