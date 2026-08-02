package org.janan.client.dto;

import java.util.List;

public record GeminiGenerationRequest(
        List<Content> contents,
        SystemInstruction systemInstruction
) {
    public record Content(String role, List<Part> parts) {
    }

    public record SystemInstruction(List<Part> parts) {
    }

    public record Part(String text) {
    }
}
