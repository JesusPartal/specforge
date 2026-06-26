package com.jesuspartal.specforge.api.controller;

import com.jesuspartal.specforge.api.dto.SpecRequest;
import com.jesuspartal.specforge.api.dto.SpecResponse;
import com.jesuspartal.specforge.api.dto.SpecSummaryResponse;
import com.jesuspartal.specforge.application.service.PostmanCollectionService;
import com.jesuspartal.specforge.application.service.SpecParserService;
import com.jesuspartal.specforge.application.service.SpecService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.jesuspartal.specforge.api.dto.TestSkeletonResponse;
import com.jesuspartal.specforge.application.service.TestSkeletonService;


import java.util.List;

@RestController
@RequestMapping("/api/specs")
@RequiredArgsConstructor
public class SpecController {

    private final SpecService specService;
    private final SpecParserService specParserService;
    private final PostmanCollectionService postmanCollectionService;
    private final TestSkeletonService testSkeletonService;


    @GetMapping
    @Operation(tags = "specs", summary = "List all specs")
    @ApiResponse(responseCode = "200", description = "Spec list")
    public ResponseEntity<List<SpecResponse>> getAllSpecs() {
        return ResponseEntity.ok(specService.getAllSpecs());
    }

    @PostMapping
    @Operation(tags = "specs", summary = "Create spec")
    @ApiResponse(responseCode = "201", description = "Spec created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public ResponseEntity<SpecResponse> createSpec(@Valid @RequestBody SpecRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specService.createSpec(request));
    }

    @PostMapping("/fetch")
    @Operation(tags = "specs", summary = "Fetch spec from GitHub")
    @Parameter(name = "repoUrl", description = "GitHub repo URL", required = true)
    @ApiResponse(responseCode = "201", description = "Spec fetched")
    @ApiResponse(responseCode = "404", description = "No spec found")
    public ResponseEntity<SpecResponse> fetchAndSaveSpec(@RequestParam String repoUrl) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specService.fetchAndSaveSpec(repoUrl));
    }

    @GetMapping("/{id}/summary")
    @Operation(tags = "specs", summary = "Summarize spec")
    @Parameter(name = "id", description = "Spec ID", required = true)
    @ApiResponse(responseCode = "200", description = "Summary")
    @ApiResponse(responseCode = "404", description = "Spec not found")
    public ResponseEntity<SpecSummaryResponse> summarizeSpec(@PathVariable Long id) {
        return ResponseEntity.ok(specParserService.summarize(id));
    }

    @GetMapping("/{id}/postman")
    @Operation(tags = "specs", summary = "Generate Postman collection")
    @Parameter(name = "id", description = "Spec ID", required = true)
    @ApiResponse(responseCode = "200", description = "Postman JSON")
    @ApiResponse(responseCode = "404", description = "Spec not found")
    public ResponseEntity<ObjectNode> generatePostmanCollection(@PathVariable Long id) {
        return ResponseEntity.ok(postmanCollectionService.generateCollection(id));
    }

    @GetMapping("/{id}/tests")
    @Operation(tags = "specs", summary = "Generate test skeleton")
    @Parameter(name = "id", description = "Spec ID", required = true)
    @ApiResponse(responseCode = "200", description = "Test skeleton Java code")
    @ApiResponse(responseCode = "404", description = "Spec not found")
    public ResponseEntity<TestSkeletonResponse> generateTests(@PathVariable Long id) {
        return ResponseEntity.ok(testSkeletonService.generateTests(id));
    }
}