package org.janan.service;

import org.janan.client.EmbeddingClient;
import org.janan.exception.NoRelevantChunkFoundException;
import org.janan.model.Chunk;
import org.janan.repository.ChunkRepository;
import org.janan.util.VectorUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RetrievalService {
    private final EmbeddingClient embeddingClient;
    private final ChunkRepository chunkRepository;
    private final int topK;
    private final double similarityThreshold;

    public RetrievalService(
            EmbeddingClient embeddingClient,
            ChunkRepository chunkRepository,
            @Value("${rag.retrieval.top-k}") int topK,
            @Value("${rag.retrieval.similarity-threshold}") double similarityThreshold) {
        this.embeddingClient = embeddingClient;
        this.chunkRepository = chunkRepository;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    public List<RetrievedChunk> retrieve(String query) {
        float[] queryEmbedding = embeddingClient.embed(query);
        String embeddingLiteral = VectorUtils.toPgVectorLiteral(queryEmbedding);

        List<Chunk> candidates = chunkRepository.findTopKSimilarChunks(embeddingLiteral, topK);

        List<RetrievedChunk> scored = candidates.stream()
                .map(chunk -> new RetrievedChunk(
                        chunk,
                        VectorUtils.cosineSimilarity(queryEmbedding, chunk.getEmbedding())))
                .filter(rc -> rc.similarityScore() >= similarityThreshold)
                .sorted((a, b) -> Double.compare(b.similarityScore(), a.similarityScore()))
                .toList();

        if (scored.isEmpty()) {
            throw new NoRelevantChunkFoundException(
                    "No chunk met the similarity threshold of " + similarityThreshold
                            + " for the given query.");
        }

        return scored;
    }
}
