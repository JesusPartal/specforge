package com.jesuspartal.specforge.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Mock request/response examples for all endpoints in a spec")
public record SpecExamplesResponse(
        @Schema(description = "API title") String title,
        @Schema(description = "Endpoint examples") List<EndpointExample> endpoints
) {
    @Schema(description = "Mock example for one endpoint")
    public record EndpointExample(
            @Schema(description = "URL path", example = "/users") String path,
            @Schema(description = "HTTP method", example = "GET") String method,
            @Schema(description = "Endpoint summary") String summary,
            @Schema(description = "Mock request body JSON") String requestBody,
            @Schema(description = "Mock response body JSON") String responseBody,
            @Schema(description = "Expected response status", example = "200") int responseStatus
    ) {}
}