package com.jesuspartal.specforge.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jesuspartal.specforge.exception.SpecNotFoundException;
import com.jesuspartal.specforge.exception.SpecParseException;
import com.jesuspartal.specforge.infrastructure.repository.SpecRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Cacheable(value = "spec-postman", key = "#specId")
public class PostmanCollectionService {

    private final SpecRepository specRepository;
    private final ObjectMapper objectMapper;

    public ObjectNode generateCollection(Long specId) {
        String rawContent = specRepository.findById(specId)
                .orElseThrow(() -> new SpecNotFoundException(specId))
                .getRawContent();

        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        OpenAPI openAPI = new OpenAPIV3Parser()
                .readContents(rawContent, null, options)
                .getOpenAPI();

        if (openAPI == null) {
            throw new SpecParseException(specId, null);
        }

        // Root collection
        ObjectNode collection = objectMapper.createObjectNode();

        // Info
        ObjectNode info = objectMapper.createObjectNode();
        info.put("name", openAPI.getInfo() != null ? openAPI.getInfo().getTitle() : "API Collection");
        info.put("schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json");
        collection.set("info", info);

        // Items (one per endpoint)
        ArrayNode items = objectMapper.createArrayNode();

        if (openAPI.getPaths() != null) {
            for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
                String path = entry.getKey();
                PathItem pathItem = entry.getValue();

                if (pathItem.getGet() != null)
                    items.add(buildItem("GET", path, pathItem.getGet().getSummary(), openAPI));
                if (pathItem.getPost() != null)
                    items.add(buildItem("POST", path, pathItem.getPost().getSummary(), openAPI));
                if (pathItem.getPut() != null)
                    items.add(buildItem("PUT", path, pathItem.getPut().getSummary(), openAPI));
                if (pathItem.getDelete() != null)
                    items.add(buildItem("DELETE", path, pathItem.getDelete().getSummary(), openAPI));
                if (pathItem.getPatch() != null)
                    items.add(buildItem("PATCH", path, pathItem.getPatch().getSummary(), openAPI));
            }
        }

        collection.set("item", items);
        return collection;
    }

    private ObjectNode buildItem(String method, String path, String summary, OpenAPI openAPI) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("name", summary != null ? summary : method + " " + path);

        // Request
        ObjectNode request = objectMapper.createObjectNode();
        request.put("method", method);

        // URL
        String baseUrl = openAPI.getServers() != null && !openAPI.getServers().isEmpty()
                ? openAPI.getServers().get(0).getUrl()
                : "{{baseUrl}}";

        ObjectNode url = objectMapper.createObjectNode();
        url.put("raw", baseUrl + path);

        // Split host and path
        ArrayNode pathSegments = objectMapper.createArrayNode();
        for (String segment : path.split("/")) {
            if (!segment.isEmpty()) {
                pathSegments.add(segment);
            }
        }
        url.set("path", pathSegments);
        request.set("url", url);

        // Headers
        ArrayNode headers = objectMapper.createArrayNode();
        ObjectNode contentTypeHeader = objectMapper.createObjectNode();
        contentTypeHeader.put("key", "Content-Type");
        contentTypeHeader.put("value", "application/json");
        headers.add(contentTypeHeader);
        request.set("header", headers);

        // Body placeholder for POST/PUT/PATCH
        if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("mode", "raw");
            body.put("raw", "{}");
            ObjectNode options = objectMapper.createObjectNode();
            ObjectNode rawOptions = objectMapper.createObjectNode();
            rawOptions.put("language", "json");
            options.set("raw", rawOptions);
            body.set("options", options);
            request.set("body", body);
        }

        item.set("request", request);
        return item;
    }
}