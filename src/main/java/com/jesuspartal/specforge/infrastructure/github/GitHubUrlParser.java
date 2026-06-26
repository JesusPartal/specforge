package com.jesuspartal.specforge.infrastructure.github;

import com.jesuspartal.specforge.exception.InvalidGitHubUrlException;

import java.util.regex.Pattern;

public class GitHubUrlParser {

    private static final Pattern URL_PATTERN =
            Pattern.compile("^https://github\\.com/([^/]+)/([^/]+?)(?:\\.git)?/?$");

    public record RepoCoordinates(String owner, String repo) {}

    public static RepoCoordinates parse(String url) {
        var matcher = URL_PATTERN.matcher(url.strip());
        if (!matcher.matches()) {
            throw new InvalidGitHubUrlException(url);
        }
        return new RepoCoordinates(matcher.group(1), matcher.group(2));
    }
}
