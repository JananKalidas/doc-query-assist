package org.janan.service;

import org.janan.exception.QueryTooVagueException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class QueryValidator {

    private static final int MIN_RAW_WORDS = 3;

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "is", "are", "was", "were", "be", "been",
            "it", "this", "that", "these", "those",
            "tell", "me", "about", "what", "who", "when", "where", "why", "how",
            "to", "of", "in", "on", "at", "for", "with", "and", "or", "but",
            "i", "you", "he", "she", "we", "they", "do", "does", "did",
            "can", "could", "would", "should", "please"
    );

    public void validate(String query){
        if(query == null || query.isBlank()) throw new QueryTooVagueException("Query must not be blank.");

        String[] rawWords = query.trim().split("\\s+");

        if (rawWords.length < MIN_RAW_WORDS) {
            throw new QueryTooVagueException(
                    "Query is too short to search against. Please ask a more complete question.");
        }

        boolean hasAtLeastOneMeaningfulWord = Arrays.stream(rawWords)
                .map(word -> word.replaceAll("[^a-zA-Z0-9]", "").toLowerCase())
                .filter(word -> !word.isBlank())
                .anyMatch(word -> !STOPWORDS.contains(word));

        if (!hasAtLeastOneMeaningfulWord) {
            throw new QueryTooVagueException(
                    "Query is too vague to search against. Please ask a more specific question.");
        }
    }
}
