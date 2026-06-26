package com.jesuspartal.specforge.infrastructure.github;

import com.jesuspartal.specforge.application.service.RateLimiterService;
import com.jesuspartal.specforge.exception.GitHubApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class GitHubClient {

    private final RestClient gitHubRestClient;
    private final RateLimiterService rateLimiterService;

    public String fetchFileContent(String owner, String repo, String filePath) {
        rateLimiterService.checkAndConsume();
        try {
            return Objects.requireNonNull(gitHubRestClient.get()
                            .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, filePath)
                            .retrieve()
                            .body(GitHubFileResponse.class))
                    .decodeContent();
        } catch (Exception e) {
            throw new GitHubApiException(
                    "GitHub API error for " + owner + "/" + repo + "/" + filePath, e);
        }
    }

    public List<GitHubRepoResponse> listOrgRepos(String org) {
        return gitHubRestClient.get()
                .uri("/orgs/{org}/repos?per_page=100&sort=updated", org)
                .retrieve()
                .body(new ParameterizedTypeReference<List<GitHubRepoResponse>>() {});
    }
}