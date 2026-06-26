package com.jesuspartal.specforge.infrastructure.github;

import com.jesuspartal.specforge.exception.InvalidGitHubUrlException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GitHubUrlParserTest {

    @Test
    void shouldParseStandardUrl() {
        GitHubUrlParser.RepoCoordinates coords = GitHubUrlParser.parse("https://github.com/owner/repo");
        assertEquals("owner", coords.owner());
        assertEquals("repo", coords.repo());
    }

    @Test
    void shouldParseUrlWithTrailingSlash() {
        GitHubUrlParser.RepoCoordinates coords = GitHubUrlParser.parse("https://github.com/owner/repo/");
        assertEquals("owner", coords.owner());
        assertEquals("repo", coords.repo());
    }

    @Test
    void shouldThrowForInvalidUrl() {
        assertThrows(InvalidGitHubUrlException.class, () -> GitHubUrlParser.parse("not-a-url"));
    }

    @Test
    void shouldThrowForUrlWithoutRepo() {
        assertThrows(InvalidGitHubUrlException.class, () -> GitHubUrlParser.parse("https://github.com/owner"));
    }
}