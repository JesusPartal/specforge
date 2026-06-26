package com.jesuspartal.specforge.application.service;

import com.jesuspartal.specforge.api.dto.SpecSummaryResponse;
import com.jesuspartal.specforge.api.dto.SpecSummaryResponse.EndpointSummary;
import com.jesuspartal.specforge.infrastructure.repository.SpecRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SpecParserService {

    private final SpecRepository specRepository;

    public SpecSummaryResponse summarize(Long specId) {
        String rawContent = specRepository.findById(specId)
                .orElseThrow(() -> new RuntimeException("Spec not found: " + specId))
                .getRawContent();

        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        OpenAPI openAPI = new OpenAPIV3Parser().readContents(rawContent, null, options).getOpenAPI();

        if (openAPI == null) {
            throw new RuntimeException("Failed to parse OpenAPI spec for id: " + specId);
        }

        String title = openAPI.getInfo() != null ? openAPI.getInfo().getTitle() : "Unknown";
        String version = openAPI.getInfo() != null ? openAPI.getInfo().getVersion() : "Unknown";
        String description = openAPI.getInfo() != null ? openAPI.getInfo().getDescription() : "";

        List<EndpointSummary> endpoints = new ArrayList<>();
        if (openAPI.getPaths() != null) {
            for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
                String path = entry.getKey();
                PathItem pathItem = entry.getValue();

                if (pathItem.getGet() != null)
                    endpoints.add(new EndpointSummary(path, "GET", pathItem.getGet().getSummary()));
                if (pathItem.getPost() != null)
                    endpoints.add(new EndpointSummary(path, "POST", pathItem.getPost().getSummary()));
                if (pathItem.getPut() != null)
                    endpoints.add(new EndpointSummary(path, "PUT", pathItem.getPut().getSummary()));
                if (pathItem.getDelete() != null)
                    endpoints.add(new EndpointSummary(path, "DELETE", pathItem.getDelete().getSummary()));
                if (pathItem.getPatch() != null)
                    endpoints.add(new EndpointSummary(path, "PATCH", pathItem.getPatch().getSummary()));
            }
        }

        List<String> schemas = new ArrayList<>();
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            schemas.addAll(openAPI.getComponents().getSchemas().keySet());
        }

        return new SpecSummaryResponse(title, version, description, endpoints, schemas);
    }
}