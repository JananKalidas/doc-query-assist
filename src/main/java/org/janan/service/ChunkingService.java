package org.janan.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    private static final double BOUNDARY_LOOKBACK_FRACTION = 0.2;

    private final int chunkSizeChars;
    private final int overlapChars;

    public ChunkingService(
            @Value("${rag.chunking.chunk-size-chars}") int chunkSizeChars,
            @Value("${rag.chunking.overlap-percent}") int overlapPercent) {
        this.chunkSizeChars = chunkSizeChars;
        this.overlapChars = (chunkSizeChars * overlapPercent) / 100;
    }

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        String trimmed = text.trim();

        // Document smaller than one chunk - no splitting needed.
        if (trimmed.length() <= chunkSizeChars) {
            chunks.add(trimmed);
            return chunks;
        }

        int start = 0;
        int lookbackWindow = (int) (chunkSizeChars * BOUNDARY_LOOKBACK_FRACTION);

        while (start < trimmed.length()) {
            int idealEnd = Math.min(start + chunkSizeChars, trimmed.length());
            int end = idealEnd;

            if (idealEnd < trimmed.length()) {
                end = findBoundary(trimmed, start, idealEnd, lookbackWindow);
            }

            chunks.add(trimmed.substring(start, end).trim());

            if (end >= trimmed.length()) {
                break;
            }

            // Slide the window forward, carrying back `overlapChars` of
            // context so meaning that straddles a chunk boundary isn't lost.
            int nextStart = end - overlapChars;
            // Guard against zero/negative progress (e.g. overlap misconfigured
            // to be >= chunk size) - always advance past the current start.
            start = Math.max(nextStart, start + 1);
        }

        return chunks;
    }

    private int findBoundary(String text, int start, int idealEnd, int lookbackWindow) {
        int searchFrom = Math.max(start, idealEnd - lookbackWindow);

        int paragraphBreak = text.lastIndexOf("\n\n", idealEnd);
        if (paragraphBreak >= searchFrom) {
            return paragraphBreak + 2;
        }

        int sentenceBreak = text.lastIndexOf(". ", idealEnd);
        if (sentenceBreak >= searchFrom) {
            return sentenceBreak + 2;
        }

        return idealEnd;
    }
}
