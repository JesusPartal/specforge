package com.jesuspartal.specforge.application.service;

import com.jesuspartal.specforge.api.dto.TestSkeletonResponse;
import com.jesuspartal.specforge.exception.SpecNotFoundException;
import com.jesuspartal.specforge.infrastructure.repository.SpecRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
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
public class TestSkeletonService {

    private final SpecRepository specRepository;
    private final ExampleGeneratorService exampleGeneratorService;

    @Cacheable(value = "spec-test-skeletons", key = "#specId")
    public TestSkeletonResponse generateTests(Long specId) {
        String rawContent = specRepository.findById(specId)
                .orElseThrow(() -> new SpecNotFoundException(specId))
                .getRawContent();

        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        OpenAPI openAPI = new OpenAPIV3Parser()
                .readContents(rawContent, null, options)
                .getOpenAPI();

        if (openAPI == null || openAPI.getInfo() == null) {
            return new TestSkeletonResponse("UnknownApiTest", "// No spec found");
        }

        String className = sanitizeClassName(openAPI.getInfo().getTitle()) + "Test";
        String baseUrl = (openAPI.getServers() != null && !openAPI.getServers().isEmpty())
                ? openAPI.getServers().get(0).getUrl()
                : "http://localhost:8080";

        StringBuilder methods = new StringBuilder();
        if (openAPI.getPaths() != null) {
            for (var entry : openAPI.getPaths().entrySet()) {
                String path = entry.getKey();
                PathItem item = entry.getValue();
                buildTestMethod("GET", path, item.getGet(), baseUrl, methods);
                buildTestMethod("POST", path, item.getPost(), baseUrl, methods);
                buildTestMethod("PUT", path, item.getPut(), baseUrl, methods);
                buildTestMethod("DELETE", path, item.getDelete(), baseUrl, methods);
                buildTestMethod("PATCH", path, item.getPatch(), baseUrl, methods);
            }
        }

        String code = buildClass(className, "com.example.generated", baseUrl, methods.toString());
        return new TestSkeletonResponse(className, code);
    }

    private void buildTestMethod(String method, String path, Operation op,
                                 String baseUrl, StringBuilder sb) {
        if (op == null) return;

        String methodName = buildMethodName(method, path);
        String summary = op.getSummary() != null ? op.getSummary() : method + " " + path;
        String escapedPath = path.replace("\"", "\\\"");
        String escapedSummary = summary.replace("\"", "\\\"");
        String bodyLine = "";
        String acceptLine = "";
        String bodyArg = "null";

        if (needsBody(method)) {
            bodyLine = "            .contentType(MediaType.APPLICATION_JSON)\n";
            bodyArg = "\"{}\"";
        }
        if ("GET".equals(method)) {
            acceptLine = "            .accept(MediaType.APPLICATION_JSON)\n";
        }

        int expectedStatus = getExpectedStatus(op);

        sb.append("""
            @Test
            @DisplayName("%s - %s")
            void %s() {
                var response = client.method()
                        .uri(BASE_URL + "%s")
                %s%s        .body(%s)
                        .retrieve()
                        .toBodilessEntity();
                assertEquals(%d, response.getStatusCode().value());
            }

            """.formatted(
                method + " " + path, escapedSummary, methodName, escapedPath,
                acceptLine, bodyLine, bodyArg, expectedStatus
        ).replace("client.method()", "client." + method.toLowerCase() + "()"));
    }

    private String buildMethodName(String method, String path) {
        String cleaned = path.replaceAll("[{}]", "")
                .replaceAll("/", "")
                .replaceAll("[^a-zA-Z0-9]", "");
        if (cleaned.isEmpty()) cleaned = "root";
        return "test" + method.charAt(0) + method.substring(1).toLowerCase()
                + cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
    }

    private boolean needsBody(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    private int getExpectedStatus(Operation op) {
        if (op.getResponses() == null) return 200;
        return op.getResponses().keySet().stream()
                .filter(k -> k.startsWith("2"))
                .findFirst()
                .map(Integer::parseInt)
                .orElse(op.getResponses().containsKey("201") ? 201 : 200);
    }

    private String sanitizeClassName(String title) {
        String cleaned = title.replaceAll("[^a-zA-Z0-9]", "");
        if (cleaned.isEmpty()) return "Api";
        return cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
    }

    private String buildClass(String className, String packageName,
                              String baseUrl, String methods) {
        return """
            package %s;

            import org.junit.jupiter.api.DisplayName;
            import org.junit.jupiter.api.Test;
            import org.springframework.http.MediaType;
            import org.springframework.web.client.RestClient;

            import static org.junit.jupiter.api.Assertions.*;

            class %s {

                private final RestClient client = RestClient.create();
                private static final String BASE_URL = "%s";

            %s}
            """.formatted(packageName, className, baseUrl, indent(methods));
    }

    private String indent(String code) {
        return code.lines()
                .map(line -> "    " + line)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }
}