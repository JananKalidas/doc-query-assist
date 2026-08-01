package org.janan.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnthropicResponse(List<ContentBlock> content) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(String type, String text){}
}
