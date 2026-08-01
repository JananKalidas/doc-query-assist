package org.janan.service;

import jakarta.transaction.Transactional;
import org.janan.client.EmbeddingClient;
import org.janan.model.Chunk;
import org.janan.model.Document;
import org.janan.repository.ChunkRepository;
import org.janan.repository.DocumentRepository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class IngestionService {

    private final TextExtractor textExtractor;
    private final ChunkingService chunkingService;
    private final EmbeddingClient embeddingClient;
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;

    public IngestionService(
            TextExtractor textExtractor,
            ChunkingService chunkingService,
            EmbeddingClient embeddingClient,
            DocumentRepository documentRepository,
            ChunkRepository chunkRepository) {
        this.textExtractor = textExtractor;
        this.chunkingService = chunkingService;
        this.embeddingClient = embeddingClient;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    @Transactional
    public Document ingest(MultipartFile file) {
        Document document = new Document();
        document.setFileName(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document = documentRepository.save(document);

        try {
            String text = extractText(file);
            List<String> chunkTexts = chunkingService.chunk(text);

            // Batch-embed all chunks from this document in one API call
            // rather than one call per chunk (Risk #11 - cost/latency).
            List<float[]> embeddings = embeddingClient.embedBatch(chunkTexts);

            List<Chunk> chunks = new ArrayList<>();
            for (int i = 0; i < chunkTexts.size(); i++) {
                chunks.add(new Chunk(document, i, chunkTexts.get(i), embeddings.get(i)));
            }
            chunkRepository.saveAll(chunks);

            document.setStatus(Document.IngestionStatus.PROCESSED);
        } catch (RuntimeException e) {
            document.setStatus(Document.IngestionStatus.FAILED);
            documentRepository.save(document);
            throw e;
        }

        return documentRepository.save(document);
    }

    private String extractText(MultipartFile file) {
        try {
            return textExtractor.extract(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file stream", e);
        }
    }
}
