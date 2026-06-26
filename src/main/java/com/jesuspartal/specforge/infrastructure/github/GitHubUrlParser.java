package com.jesuspartal.specforge.infrastructure.github;

import com.jesuspartal.specforge.exception.InvalidGitHubUrlException;

public class GitHubUrlParser {

    public record RepoCoordinates(String owner, String repo) {}

    public static RepoCoordinates parse(String url) {
        // https://github.com/owner/repo → [owner, repo]
        String cleaned = url
                .replace("https://github.com/", "")
                .replace("http://github.com/", "")
                .replaceAll("/$", "");

        String[] parts = cleaned.split("/");

        if (parts.length < 2) {
            throw new InvalidGitHubUrlException(url);
        }

        return new RepoCoordinates(parts[0], parts[1]);
    }
}