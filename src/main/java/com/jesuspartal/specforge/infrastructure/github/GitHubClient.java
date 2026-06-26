package com.jesuspartal.specforge.infrastructure.github;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class GitHubClient {

    private final WebClient gitHubWebClient;

    public String fetchFileContent(String owner, String repo, String filePath) {
        return gitHubWebClient.get()
                .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, filePath)
                .retrieve()
                .bodyToMono(GitHubFileResponse.class)
                .map(GitHubFileResponse::decodeContent)
                .block();
    }
}