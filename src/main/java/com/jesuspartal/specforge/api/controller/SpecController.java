package com.jesuspartal.specforge.api.controller;

import com.jesuspartal.specforge.api.dto.SpecRequest;
import com.jesuspartal.specforge.api.dto.SpecResponse;
import com.jesuspartal.specforge.api.dto.SpecSummaryResponse;
import com.jesuspartal.specforge.application.service.SpecParserService;
import com.jesuspartal.specforge.application.service.SpecService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/specs")
@RequiredArgsConstructor
public class SpecController {

    private final SpecService specService;
    private final SpecParserService specParserService;

    @GetMapping
    public ResponseEntity<List<SpecResponse>> getAllSpecs() {
        return ResponseEntity.ok(specService.getAllSpecs());
    }

    @PostMapping
    public ResponseEntity<SpecResponse> createSpec(@Valid @RequestBody SpecRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specService.createSpec(request));
    }

    @PostMapping("/fetch")
    public ResponseEntity<SpecResponse> fetchAndSaveSpec(@RequestParam String repoUrl) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specService.fetchAndSaveSpec(repoUrl));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<SpecSummaryResponse> summarizeSpec(@PathVariable Long id) {
        return ResponseEntity.ok(specParserService.summarize(id));
    }
}