package com.jesuspartal.specforge.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jesuspartal.specforge.api.dto.SpecExamplesResponse;
import com.jesuspartal.specforge.api.dto.SpecExamplesResponse.EndpointExample;
import com.jesuspartal.specforge.exception.SpecNotFoundException;
import com.jesuspartal.specforge.infrastructure.repository.SpecRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExampleGeneratorService {

    private final SpecRepository specRepository;
    private final ObjectMapper objectMapper;

    @Cacheable(value = "spec-examples", key = "#specId")
    public SpecExamplesResponse generateExamples(Long specId) {
        String rawContent = specRepository.findById(specId)
                .orElseThrow(() -> new SpecNotFoundException(specId))
                .getRawContent();

        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        OpenAPI openAPI = new OpenAPIV3Parser()
                .readContents(rawContent, null, options)
                .getOpenAPI();

        if (openAPI == null) {
            return new SpecExamplesResponse("Unknown", List.of());
        }

        String title = openAPI.getInfo() != null ? openAPI.getInfo().getTitle() : "Unknown";
        Map<String, Schema> schemas = openAPI.getComponents() != null
                && openAPI.getComponents().getSchemas() != null
                ? openAPI.getComponents().getSchemas()
                : Map.of();

        List<EndpointExample> endpoints = new ArrayList<>();
        if (openAPI.getPaths() != null) {
            for (var entry : openAPI.getPaths().entrySet()) {
                String path = entry.getKey();
                PathItem item = entry.getValue();
                processOperation("GET", path, item.getGet(), schemas, endpoints);
                processOperation("POST", path, item.getPost(), schemas, endpoints);
                processOperation("PUT", path, item.getPut(), schemas, endpoints);
                processOperation("DELETE", path, item.getDelete(), schemas, endpoints);
                processOperation("PATCH", path, item.getPatch(), schemas, endpoints);
            }
        }

        return new SpecExamplesResponse(title, endpoints);
    }

    private void processOperation(String method, String path, Operation op,
                                  Map<String, Schema> schemas, List<EndpointExample> results) {
        if (op == null) return;

        String summary = op.getSummary() != null ? op.getSummary() : "";
        String requestBody = null;
        String responseBody = null;
        int status = 200;

        if (op.getRequestBody() != null && op.getRequestBody().getContent() != null) {
            var mediaType = op.getRequestBody().getContent().get("application/json");
            if (mediaType != null && mediaType.getSchema() != null) {
                Schema schema = resolveSchema(mediaType.getSchema(), schemas);
                requestBody = mockToJson(generateMockValue(schema, schemas));
            }
        }

        if (op.getResponses() != null) {
            var firstSuccess = op.getResponses().keySet().stream()
                    .filter(k -> k.startsWith("2"))
                    .findFirst()
                    .orElse("200");
            try {
                status = Integer.parseInt(firstSuccess);
            } catch (NumberFormatException ignored) {}
        }

        if (op.getResponses() != null) {
            for (var respEntry : op.getResponses().entrySet()) {
                var resp = respEntry.getValue();
                if (resp.getContent() != null && resp.getContent().get("application/json") != null) {
                    var mediaType = resp.getContent().get("application/json");
                    if (mediaType.getSchema() != null) {
                        Schema schema = resolveSchema(mediaType.getSchema(), schemas);
                        responseBody = mockToJson(generateMockValue(schema, schemas));
                        break;
                    }
                }
            }
        }

        results.add(new EndpointExample(path, method, summary, requestBody, responseBody, status));
    }

    private Schema resolveSchema(Schema schema, Map<String, Schema> schemas) {
        if (schema == null) return null;
        if (schema.get$ref() != null) {
            String refName = schema.get$ref().replace("#/components/schemas/", "");
            Schema resolved = schemas.get(refName);
            return resolved != null ? resolved : schema;
        }
        return schema;
    }

    private JsonNode generateMockValue(Schema schema, Map<String, Schema> schemas) {
        schema = resolveSchema(schema, schemas);
        if (schema == null) return objectMapper.valueToTree("value");

        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            return objectMapper.valueToTree(schema.getEnum().get(0));
        }

        String type = schema.getType();
        String format = schema.getFormat();

        if ("object".equals(type)) {
            ObjectNode obj = objectMapper.createObjectNode();
            if (schema.getProperties() != null) {
                Map<String, Schema> props = schema.getProperties();
                for (Map.Entry<String, Schema> prop : props.entrySet()) {
                    obj.set(prop.getKey(), generateMockValue(prop.getValue(), schemas));
                }
            }
            return obj;
        }

        if ("array".equals(type)) {
            ArrayNode arr = objectMapper.createArrayNode();
            if (schema.getItems() != null) {
                arr.add(generateMockValue(schema.getItems(), schemas));
            }
            return arr;
        }

        if ("string".equals(type)) {
            return objectMapper.valueToTree(switch (format) {
                case "date" -> "2026-01-01";
                case "date-time", "datetime" -> "2026-01-01T00:00:00Z";
                case "email" -> "user@example.com";
                case "uri", "url" -> "https://example.com";
                case "uuid" -> "550e8400-e29b-41d4-a716-446655440000";
                default -> "string";
            });
        }

        if ("number".equals(type)) return objectMapper.valueToTree(1.0);
        if ("integer".equals(type)) return objectMapper.valueToTree(1);
        if ("boolean".equals(type)) return objectMapper.valueToTree(true);

        return objectMapper.valueToTree("value");
    }

    private String mockToJson(JsonNode node) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }
}