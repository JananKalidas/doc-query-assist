package org.janan.integration;

import org.janan.client.AnthropicClient;
import org.janan.client.EmbeddingClient;
import org.janan.dto.AskRequest;
import org.janan.dto.AskResponse;
import org.janan.dto.UploadResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DocumentIngestionAndQueryIntegrationTest {
    private static final int EMBEDDING_DIMENSIONS = 1536;

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("pgvector/pgvector:pg16");
//            DockerImageName.parse("pgvector/pgvector:pg16")
//                    .asCompatibleSubstituteFor("postgres"))
//            .withDatabaseName("ragdb_test")
//            .withUsername("testuser")
//            .withPassword("testpass")
//            .withInitScript("init-test-db.sql");

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @org.springframework.beans.factory.annotation.Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private EmbeddingClient embeddingClient;

    @MockitoBean
    private AnthropicClient anthropicClient;

    @Test
    void uploadThenAsk_returnsGroundedAnswerWithSources() {
        float[] fixedEmbedding = fixedVector();

        // Same vector for the stored chunk and the incoming query ->
        // cosine similarity of 1.0, well above the default 0.80 threshold.
        when(embeddingClient.embedBatch(any())).thenReturn(List.of(fixedEmbedding));
        when(embeddingClient.embed(anyString())).thenReturn(fixedEmbedding);
        when(anthropicClient.generate(any())).thenReturn(
                "Refunds are available within 30 days of purchase.");

        String documentText = "Our refund policy allows returns within 30 days of purchase, "
                + "provided the item is unused and in its original packaging.";

        UploadResponse uploadResponse = uploadDocument("policy.txt", documentText);

        assertThat(uploadResponse.documentId()).isNotNull();
        assertThat(uploadResponse.status()).isEqualTo("PROCESSED");

        AskResponse askResponse = askQuestion("What is the refund policy?");

        assertThat(askResponse.answer())
                .isEqualTo("Refunds are available within 30 days of purchase.");
        assertThat(askResponse.sources()).isNotEmpty();
        assertThat(askResponse.sources().get(0).document()).isEqualTo("policy.txt");
        assertThat(askResponse.sources().get(0).score()).isGreaterThanOrEqualTo(0.80);
    }

    @Test
    void ask_withNoDocumentsUploaded_returns422() {
        float[] fixedEmbedding = fixedVector();
        when(embeddingClient.embed(anyString())).thenReturn(fixedEmbedding);

        String url = "http://localhost:" + port + "/api/ask";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AskRequest> request = new HttpEntity<>(
                new AskRequest("Is there any content to match against?"), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).contains("NO_RELEVANT_CHUNK");
    }

    private UploadResponse uploadDocument(String fileName, String content) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource fileResource = new ByteArrayResource(
                content.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        body.add("file", fileResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String url = "http://localhost:" + port + "/api/documents/upload";
        ResponseEntity<UploadResponse> response =
                restTemplate.postForEntity(url, requestEntity, UploadResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private AskResponse askQuestion(String question) {
        String url = "http://localhost:" + port + "/api/ask";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AskRequest> request = new HttpEntity<>(new AskRequest(question), headers);

        ResponseEntity<AskResponse> response =
                restTemplate.postForEntity(url, request, AskResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private float[] fixedVector() {
        float[] vector = new float[EMBEDDING_DIMENSIONS];
        for (int i = 0; i < EMBEDDING_DIMENSIONS; i++) {
            vector[i] = 0.01f;
        }
        return vector;
    }
}
