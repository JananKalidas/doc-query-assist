package org.janan.exception;

public class QueryTooVagueException extends RuntimeException{
    public QueryTooVagueException(String message) {
        super(message);
    }
}
