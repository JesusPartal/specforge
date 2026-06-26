package com.jesuspartal.specforge.application.service;

import com.jesuspartal.specforge.api.dto.SpecRequest;
import com.jesuspartal.specforge.api.dto.SpecResponse;
import com.jesuspartal.specforge.domain.model.Spec;
import com.jesuspartal.specforge.exception.SpecNotFoundException;
import com.jesuspartal.specforge.infrastructure.github.GitHubUrlParser;
import com.jesuspartal.specforge.infrastructure.github.GitHubUrlParser.RepoCoordinates;
import com.jesuspartal.specforge.infrastructure.github.SpecFileFinder;
import com.jesuspartal.specforge.infrastructure.github.SpecFileFinder.FoundSpec;
import com.jesuspartal.specforge.infrastructure.repository.SpecRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecService {

    private final SpecRepository specRepository;
    private final SpecFileFinder specFileFinder;

    @Cacheable(value = "specs-all")
    public List<SpecResponse> getAllSpecs() {
        return specRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @CacheEvict(value = "specs-all", allEntries = true)
    public SpecResponse createSpec(SpecRequest request) {
        Spec spec = Spec.builder()
                .repoUrl(request.repoUrl())
                .title(request.title())
                .version(request.version())
                .rawContent(request.rawContent())
                .fetchedAt(LocalDateTime.now())
                .build();

        return toResponse(specRepository.save(spec));
    }

    @CacheEvict(value = "specs-all", allEntries = true)
    public SpecResponse fetchAndSaveSpec(String repoUrl) {
        RepoCoordinates coords = GitHubUrlParser.parse(repoUrl);

        FoundSpec foundSpec = specFileFinder
                .findSpec(coords.owner(), coords.repo())
                .orElseThrow(() -> new SpecNotFoundException(
                        "No OpenAPI spec found in repository: " + repoUrl
                ));

        Spec spec = Spec.builder()
                .repoUrl(repoUrl)
                .title(coords.owner() + "/" + coords.repo())
                .version("unknown")
                .rawContent(foundSpec.content())
                .fetchedAt(LocalDateTime.now())
                .build();

        return toResponse(specRepository.save(spec));
    }

    private SpecResponse toResponse(Spec spec) {
        return new SpecResponse(
                spec.getId(),
                spec.getRepoUrl(),
                spec.getTitle(),
                spec.getVersion(),
                spec.getFetchedAt()
        );
    }
}