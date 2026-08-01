package org.janan.service;

import org.janan.model.Chunk;

public record RetrievedChunk(Chunk chunk,
                             double similarityScore) {
}
