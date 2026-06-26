package com.jesuspartal.specforge.infrastructure.github;

import java.util.Base64;

public record GitHubFileResponse(
        String name,
        String path,
        String content,
        String encoding
) {
    public String decodeContent() {
        String cleaned = content.replaceAll("\\s", "");
        return new String(Base64.getDecoder().decode(cleaned));
    }
}