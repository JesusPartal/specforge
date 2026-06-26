package com.jesuspartal.specforge.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Diff between two spec versions")
public record SpecDiffResponse(
        @Schema(description = "Old spec title") String oldTitle,
        @Schema(description = "Old spec version") String oldVersion,
        @Schema(description = "New spec title") String newTitle,
        @Schema(description = "New spec version") String newVersion,
        @Schema(description = "List of detected changes") List<Change> changes,
        @Schema(description = "Number of breaking changes") int breakingCount
) {
    public record Change(
            @Schema(description = "Endpoint path, param name or schema name") String location,
            @Schema(description = "BREAKING | NON_BREAKING | INFO") String severity,
            @Schema(description = "ENDPOINT_ADDED | ENDPOINT_REMOVED | ENDPOINT_MODIFIED | PARAM_* | RESPONSE_* | SCHEMA_*")
            String type,
            @Schema(description = "Human-readable description") String description
    ) {}
}