package com.jesuspartal.specforge.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SpecRequest(
        @NotBlank String repoUrl,
        @NotBlank String title,
        String version,
        String rawContent
) {}