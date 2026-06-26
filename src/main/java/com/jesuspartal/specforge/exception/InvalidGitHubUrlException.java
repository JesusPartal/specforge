package com.jesuspartal.specforge.exception;

public class InvalidGitHubUrlException extends RuntimeException{
    public InvalidGitHubUrlException(String url) {
        super("Invalid GitHub URL: " + url);
    }
}
