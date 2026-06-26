package com.jesuspartal.specforge.infrastructure.github;

public record GitHubRepoResponse (
    String name,
    String fullName,
    String htmlUrl
) {}
