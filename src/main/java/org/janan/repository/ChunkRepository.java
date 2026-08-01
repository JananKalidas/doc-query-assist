package org.janan.repository;

import org.janan.model.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChunkRepository extends JpaRepository<Chunk, UUID> {

    @Query(value = """
            SELECT * FROM chunks c
            ORDER BY c.embedding <=> CAST(:embedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<Chunk> findTopKSimilarChunks(@Param("embedding") String embeddingLiteral, @Param("topK") int topK);

    List<Chunk> findByDocumentId(UUID documentId);
}
