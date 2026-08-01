package org.janan.exception;

public class UnsupportedFileTypeException extends RuntimeException{

    public UnsupportedFileTypeException(String message) {
        super(message);
    }

    public UnsupportedFileTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
