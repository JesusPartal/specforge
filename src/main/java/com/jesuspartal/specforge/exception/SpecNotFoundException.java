package com.jesuspartal.specforge.exception;

public class SpecNotFoundException extends RuntimeException {
    public SpecNotFoundException(Long id) {
        super("Spec not found: "  + id);
    }

    public SpecNotFoundException(String message) {
        super(message);
    }
}

