package com.jesuspartal.specforge.infrastructure.github;

import com.jesuspartal.specforge.exception.GitHubApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpecFileFinder {

    private final GitHubClient gitHubClient;

    private static final List<String> CANDIDATE_PATHS = List.of(
            "openapi.yaml",
            "openapi.json",
            "openapi.yml",
            "docs/openapi.yaml",
            "docs/openapi.json",
            "api/openapi.yaml",
            "api/openapi.json",
            "swagger.yaml",
            "swagger.json",
            "src/main/resources/openapi.yaml"
    );

    public Optional<FoundSpec> findSpec(String owner, String repo) {
        for (String path : CANDIDATE_PATHS) {
            try {
                String content = gitHubClient.fetchFileContent(owner, repo, path);
                if (content != null && !content.isBlank()) {
                    return Optional.of(new FoundSpec(path, content));
                }
            } catch (GitHubApiException e) {
                log.debug("Spec file not found at path: {}", path);
            }
        }
        return Optional.empty();
    }

    public record FoundSpec(String path, String content) {}
}