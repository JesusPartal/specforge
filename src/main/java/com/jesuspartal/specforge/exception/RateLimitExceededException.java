package com.jesuspartal.specforge.exception;

public class RateLimitExceededException extends RuntimeException{
    public RateLimitExceededException() {
        super("Rate limit Exceed. Please try again later.");
    }
}
