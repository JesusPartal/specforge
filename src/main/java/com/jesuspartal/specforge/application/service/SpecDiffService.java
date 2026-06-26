package com.jesuspartal.specforge.application.service;

import com.jesuspartal.specforge.api.dto.SpecDiffResponse;
import com.jesuspartal.specforge.api.dto.SpecDiffResponse.Change;
import com.jesuspartal.specforge.domain.model.Spec;
import com.jesuspartal.specforge.exception.SpecNotFoundException;
import com.jesuspartal.specforge.exception.SpecParseException;
import com.jesuspartal.specforge.infrastructure.repository.SpecRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SpecDiffService {

    private final SpecRepository specRepository;

    @Cacheable(value = "spec-diff", key = "{#oldSpecId, #newSpecId}")
    public SpecDiffResponse diff(Long oldSpecId, Long newSpecId) {
        Spec oldSpec = specRepository.findById(oldSpecId)
                .orElseThrow(() -> new SpecNotFoundException(oldSpecId));
        Spec newSpec = specRepository.findById(newSpecId)
                .orElseThrow(() -> new SpecNotFoundException(newSpecId));

        OpenAPI oldApi = parse(oldSpec.getRawContent(), oldSpecId);
        OpenAPI newApi = parse(newSpec.getRawContent(), newSpecId);

        List<Change> changes = new ArrayList<>();

        String oldTitle = info(oldApi, "title", "");
        String oldVer = info(oldApi, "version", "");
        String newTitle = info(newApi, "title", "");
        String newVer = info(newApi, "version", "");

        if (!Objects.equals(oldTitle, newTitle)) {
            changes.add(new Change("info", "INFO", "TITLE_CHANGED",
                    "Title: '" + oldTitle + "' → '" + newTitle + "'"));
        }

        compareEndpoints(oldApi, newApi, changes);
        compareSchemas(oldApi, newApi, changes);

        int breaking = (int) changes.stream().filter(c -> "BREAKING".equals(c.severity())).count();
        return new SpecDiffResponse(oldTitle, oldVer, newTitle, newVer, changes, breaking);
    }

    private OpenAPI parse(String content, Long specId) {
        ParseOptions opts = new ParseOptions();
        opts.setResolve(true);
        OpenAPI api = new OpenAPIV3Parser().readContents(content, null, opts).getOpenAPI();
        if (api == null) throw new SpecParseException(specId, null);
        return api;
    }

    private String info(OpenAPI api, String field, String defaultVal) {
        if (api.getInfo() == null) return defaultVal;
        return switch (field) {
            case "title" -> api.getInfo().getTitle() != null ? api.getInfo().getTitle() : defaultVal;
            case "version" -> api.getInfo().getVersion() != null ? api.getInfo().getVersion() : defaultVal;
            default -> defaultVal;
        };
    }

    private void compareEndpoints(OpenAPI oldApi, OpenAPI newApi, List<Change> changes) {
        Map<String, PathItem> oldPaths = oldApi.getPaths() != null ? oldApi.getPaths() : Map.of();
        Map<String, PathItem> newPaths = newApi.getPaths() != null ? newApi.getPaths() : Map.of();

        Set<String> oldKeys = collectMethodPaths(oldPaths);
        Set<String> newKeys = collectMethodPaths(newPaths);

        for (String key : oldKeys) {
            if (!newKeys.contains(key)) {
                changes.add(new Change(key, "BREAKING", "ENDPOINT_REMOVED", "Endpoint removed"));
            }
        }
        for (String key : newKeys) {
            if (!oldKeys.contains(key)) {
                changes.add(new Change(key, "NON_BREAKING", "ENDPOINT_ADDED", "Endpoint added"));
            }
        }

        for (String key : oldKeys) {
            if (!newKeys.contains(key)) continue;
            String[] parts = key.split(" ", 2);
            String method = parts[0];
            String path = parts[1];
            Operation oldOp = getOp(oldPaths.get(path), method);
            Operation newOp = getOp(newPaths.get(path), method);
            if (oldOp != null && newOp != null) {
                compareOperation(key, oldOp, newOp, changes);
            }
        }
    }

    private Set<String> collectMethodPaths(Map<String, PathItem> paths) {
        Set<String> set = new HashSet<>();
        for (var e : paths.entrySet()) {
            String p = e.getKey();
            PathItem item = e.getValue();
            if (item.getGet() != null) set.add("GET " + p);
            if (item.getPost() != null) set.add("POST " + p);
            if (item.getPut() != null) set.add("PUT " + p);
            if (item.getDelete() != null) set.add("DELETE " + p);
            if (item.getPatch() != null) set.add("PATCH " + p);
        }
        return set;
    }

    private Operation getOp(PathItem item, String method) {
        return switch (method) {
            case "GET" -> item.getGet();
            case "POST" -> item.getPost();
            case "PUT" -> item.getPut();
            case "DELETE" -> item.getDelete();
            case "PATCH" -> item.getPatch();
            default -> null;
        };
    }

    private void compareOperation(String key, Operation oldOp, Operation newOp, List<Change> changes) {
        compareParams(key, oldOp.getParameters(), newOp.getParameters(), changes);
        compareRequestBody(key, oldOp.getRequestBody() != null ? oldOp.getRequestBody().getContent() : null,
                newOp.getRequestBody() != null ? newOp.getRequestBody().getContent() : null, changes);
        compareResponses(key, oldOp.getResponses(), newOp.getResponses(), changes);
    }

    private void compareParams(String key, List<Parameter> oldParams, List<Parameter> newParams, List<Change> changes) {
        Map<String, Parameter> oldMap = paramMap(oldParams);
        Map<String, Parameter> newMap = paramMap(newParams);

        for (var e : oldMap.entrySet()) {
            String paramKey = key + " → param:" + e.getKey();
            if (!newMap.containsKey(e.getKey())) {
                changes.add(new Change(paramKey, "BREAKING", "PARAM_REMOVED",
                        "Parameter '" + e.getKey() + "' removed"));
            }
        }
        for (var e : newMap.entrySet()) {
            String paramKey = key + " → param:" + e.getKey();
            Parameter newP = e.getValue();
            if (!oldMap.containsKey(e.getKey())) {
                String sev = Boolean.TRUE.equals(newP.getRequired()) ? "BREAKING" : "NON_BREAKING";
                changes.add(new Change(paramKey, sev, "PARAM_ADDED",
                        "Parameter '" + e.getKey() + "' added" + (Boolean.TRUE.equals(newP.getRequired()) ? " (required)" : "")));
            } else {
                Parameter oldP = oldMap.get(e.getKey());
                if (!Objects.equals(oldP.getRequired(), newP.getRequired())) {
                    String sev = Boolean.TRUE.equals(newP.getRequired()) ? "BREAKING" : "NON_BREAKING";
                    changes.add(new Change(paramKey, sev, "PARAM_REQUIRED_CHANGED",
                            "Required: " + oldP.getRequired() + " → " + newP.getRequired()));
                }
            }
        }
    }

    private Map<String, Parameter> paramMap(List<Parameter> params) {
        Map<String, Parameter> map = new LinkedHashMap<>();
        if (params != null) {
            for (Parameter p : params) {
                map.put(p.getName() + "_" + p.getIn(), p);
            }
        }
        return map;
    }

    private void compareRequestBody(String key, Map<String, MediaType> oldContent,
                                    Map<String, MediaType> newContent, List<Change> changes) {
        boolean oldHasBody = oldContent != null && oldContent.containsKey("application/json");
        boolean newHasBody = newContent != null && newContent.containsKey("application/json");
        if (oldHasBody && !newHasBody) {
            changes.add(new Change(key, "BREAKING", "REQUEST_BODY_REMOVED", "Request body removed"));
        } else if (!oldHasBody && newHasBody) {
            changes.add(new Change(key, "BREAKING", "REQUEST_BODY_ADDED", "Request body required"));
        }
    }

    private void compareResponses(String key, io.swagger.v3.oas.models.responses.ApiResponses oldR,
                                  io.swagger.v3.oas.models.responses.ApiResponses newR, List<Change> changes) {
        Set<String> oldCodes = oldR != null ? oldR.keySet() : Set.of();
        Set<String> newCodes = newR != null ? newR.keySet() : Set.of();
        for (String code : oldCodes) {
            if (!newCodes.contains(code)) {
                changes.add(new Change(key, "BREAKING", "RESPONSE_REMOVED", "Response " + code + " removed"));
            }
        }
        for (String code : newCodes) {
            if (!oldCodes.contains(code)) {
                changes.add(new Change(key, "NON_BREAKING", "RESPONSE_ADDED", "Response " + code + " added"));
            }
        }
    }

    private void compareSchemas(OpenAPI oldApi, OpenAPI newApi, List<Change> changes) {
        Map<String, Schema> oldSchemas = (oldApi.getComponents() != null && oldApi.getComponents().getSchemas() != null)
                ? oldApi.getComponents().getSchemas() : Map.of();
        Map<String, Schema> newSchemas = (newApi.getComponents() != null && newApi.getComponents().getSchemas() != null)
                ? newApi.getComponents().getSchemas() : Map.of();

        for (String name : oldSchemas.keySet()) {
            if (!newSchemas.containsKey(name)) {
                changes.add(new Change("schema:" + name, "BREAKING", "SCHEMA_REMOVED", "Schema removed"));
            }
        }
        for (String name : newSchemas.keySet()) {
            if (!oldSchemas.containsKey(name)) {
                changes.add(new Change("schema:" + name, "NON_BREAKING", "SCHEMA_ADDED", "Schema added"));
            }
        }
        for (String name : oldSchemas.keySet()) {
            if (newSchemas.containsKey(name)) {
                compareSchemaProperties("schema:" + name, oldSchemas.get(name), newSchemas.get(name), changes);
            }
        }
    }

    private void compareSchemaProperties(String key, Schema oldSchema, Schema newSchema, List<Change> changes) {
        if (!"object".equals(oldSchema.getType()) || !"object".equals(newSchema.getType())) return;
        Map<String, Schema> oldProps = oldSchema.getProperties() != null ? oldSchema.getProperties() : Map.of();
        Map<String, Schema> newProps = newSchema.getProperties() != null ? newSchema.getProperties() : Map.of();

        for (String prop : oldProps.keySet()) {
            String propKey = key + "." + prop;
            if (!newProps.containsKey(prop)) {
                changes.add(new Change(propKey, "BREAKING", "SCHEMA_PROPERTY_REMOVED", "Property removed"));
            }
        }
        for (String prop : newProps.keySet()) {
            String propKey = key + "." + prop;
            if (!oldProps.containsKey(prop)) {
                changes.add(new Change(propKey, "NON_BREAKING", "SCHEMA_PROPERTY_ADDED", "Property added"));
            } else {
                Schema oldP = oldProps.get(prop);
                Schema newP = newProps.get(prop);
                if (!Objects.equals(oldP.getType(), newP.getType())) {
                    changes.add(new Change(propKey, "BREAKING", "SCHEMA_PROPERTY_TYPE_CHANGED",
                            "Type: " + oldP.getType() + " → " + newP.getType()));
                }
            }
        }

        List<String> oldRequired = oldSchema.getRequired() != null ? oldSchema.getRequired() : List.of();
        List<String> newRequired = newSchema.getRequired() != null ? newSchema.getRequired() : List.of();
        for (String prop : oldRequired) {
            if (!newRequired.contains(prop)) {
                changes.add(new Change(key + "." + prop, "NON_BREAKING", "SCHEMA_PROPERTY_OPTIONAL",
                        "Property '" + prop + "' is now optional"));
            }
        }
        for (String prop : newRequired) {
            if (!oldRequired.contains(prop)) {
                changes.add(new Change(key + "." + prop, "BREAKING", "SCHEMA_PROPERTY_REQUIRED",
                        "Property '" + prop + "' is now required"));
            }
        }
    }
}