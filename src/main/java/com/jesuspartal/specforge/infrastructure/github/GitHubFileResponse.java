package com.jesuspartal.specforge.infrastructure.github;

import com.jesuspartal.specforge.exception.GitHubApiException;

import java.util.Base64;

public record GitHubFileResponse(
        String name,
        String path,
        String content,
        String encoding
) {
    public String decodeContent() {
        if (content == null) {
            throw new GitHubApiException("File content is null for path: " + path, null);
        }
        return new String(Base64.getMimeDecoder().decode(content));
    }
}
