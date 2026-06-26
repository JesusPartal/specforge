package com.jesuspartal.specforge.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Organization spec scan result")
public record OrgScanResponse(
        @Schema(description = "Organization name") String org,
        @Schema(description = "Total repos scanned") int totalRepos,
        @Schema(description = "Repos with an OpenAPI spec") List<RepoWithSpec> withSpec,
        @Schema(description = "Repos without an OpenAPI spec") List<RepoWithoutSpec> withoutSpec
) {
    public record RepoWithSpec(String repo, String specPath, String htmlUrl) {}
    public record RepoWithoutSpec(String repo, String htmlUrl) {}
}