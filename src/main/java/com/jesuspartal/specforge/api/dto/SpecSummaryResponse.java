package com.jesuspartal.specforge.api.dto;

import java.util.List;

public record SpecSummaryResponse(
        String title,
        String version,
        String description,
        List<EndpointSummary> endpoints,
        List<String> schemas
) {
    public record EndpointSummary(
            String path,
            String method,
            String summary
    ) {}
}