package org.janan.dto;

import org.janan.service.RetrievedChunk;

import java.util.List;
import java.util.UUID;

public record AskResponse(
        String question,
        String answer,
        List<SourceChunk> sources
) {
    public record SourceChunk(
            String document,
            UUID chunkId,
            double score
    ){}

    public static AskResponse of(
            String question,
            String answer,
            List<RetrievedChunk> retrieved
    ){
        List<SourceChunk> sources = retrieved.stream()
                .map(rc -> new SourceChunk(
                        rc.chunk().getDocument().getFileName(),
                        rc.chunk().getId(),
                        round(rc.similarityScore())))
                .toList();
        return new AskResponse(question, answer, sources);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
