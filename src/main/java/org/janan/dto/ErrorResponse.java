package org.janan.dto;

import java.time.Instant;

public record ErrorResponse(
        String error,
        String message,
        int status,
        Instant timeStamp
) {
    public static ErrorResponse of(
            String error, String message, int status
    ){
        return new ErrorResponse(error, message, status, Instant.now());
    }
}
