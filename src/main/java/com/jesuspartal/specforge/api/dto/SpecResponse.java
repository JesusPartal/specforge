package com.jesuspartal.specforge.api.dto;

import java.time.LocalDateTime;

public record SpecResponse(
        Long id,
        String repoUrl,
        String title,
        String version,
        LocalDateTime fetchedAt
) {}