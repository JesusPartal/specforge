package com.jesuspartal.specforge.exception;

public class SpecParseException extends RuntimeException{
    public SpecParseException(Long id, Throwable cause) {
        super("Failed to parse OpenAPI spec for id: " + id, cause);
    }
}
