package org.janan.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.janan.exception.UnsupportedFileTypeException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

@Component
public class TextExtractor {

    private static final Pattern MULTIPLE_BLANK_LINES = Pattern.compile("\\n{3,}");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("[ \\t]{2,}");

    private final Tika tika = new Tika();

    public String extract(InputStream inputStream, String fileName) {
        String rawText;
        try {
            rawText = tika.parseToString(inputStream);
        } catch (IOException | TikaException e) {
            throw new UnsupportedFileTypeException(
                    "Could not extract text from file: " + fileName, e);
        }

        if (rawText == null || rawText.isBlank()) {
            throw new UnsupportedFileTypeException(
                    "No extractable text content found in file: " + fileName);
        }

        return normalize(rawText);
    }

    private String normalize(String text) {
        String normalized = MULTIPLE_SPACES.matcher(text).replaceAll(" ");
        normalized = MULTIPLE_BLANK_LINES.matcher(normalized).replaceAll("\n\n");
        return normalized.trim();
    }
}
