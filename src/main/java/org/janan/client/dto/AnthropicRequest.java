package org.janan.client.dto;

import java.util.List;

public record AnthropicRequest(String model,
                               int max_tokens,
                               String system,
                               List<AnthropicMessage> messages) {
    public record AnthropicMessage(String role, String content) {
    }
}
