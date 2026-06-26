package com.jesuspartal.specforge.application.service;

import com.jesuspartal.specforge.api.dto.SpecRequest;
import com.jesuspartal.specforge.domain.model.Spec;
import com.jesuspartal.specforge.exception.SpecNotFoundException;
import com.jesuspartal.specforge.infrastructure.github.SpecFileFinder;
import com.jesuspartal.specforge.infrastructure.github.SpecFileFinder.FoundSpec;
import com.jesuspartal.specforge.infrastructure.repository.SpecRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecServiceTest {

    @Mock
    private SpecRepository specRepository;
    @Mock
    private SpecFileFinder specFileFinder;

    @InjectMocks
    private SpecService service;

    @Test
    void shouldCreateSpec() {
        Spec spec = Spec.builder()
                .id(1L).repoUrl("https://github.com/test/api").title("Test").version("1.0")
                .fetchedAt(LocalDateTime.now()).build();
        when(specRepository.save(any())).thenReturn(spec);

        var result = service.createSpec(new SpecRequest("https://github.com/test/api", "Test", "1.0", null));

        assertEquals(1L, result.id());
        assertEquals("Test", result.title());
    }

    @Test
    void shouldGetAllSpecs() {
        Spec spec = Spec.builder()
                .id(1L).repoUrl("https://github.com/test/api").title("Test").version("1.0")
                .fetchedAt(LocalDateTime.now()).build();
        when(specRepository.findAll()).thenReturn(List.of(spec));

        var results = service.getAllSpecs();

        assertEquals(1, results.size());
        assertEquals("Test", results.get(0).title());
    }

    @Test
    void shouldFetchAndSaveSpec() {
        String url = "https://github.com/owner/repo";
        when(specFileFinder.findSpec("owner", "repo"))
                .thenReturn(Optional.of(new FoundSpec("openapi.yaml", "content")));
        Spec spec = Spec.builder()
                .id(1L).repoUrl(url).title("owner/repo").version("unknown")
                .rawContent("content").fetchedAt(LocalDateTime.now()).build();
        when(specRepository.save(any())).thenReturn(spec);

        var result = service.fetchAndSaveSpec(url);

        assertEquals("owner/repo", result.title());
        assertEquals("unknown", result.version());
    }

    @Test
    void shouldThrowWhenSpecNotFoundInRepo() {
        String url = "https://github.com/owner/repo";
        when(specFileFinder.findSpec("owner", "repo")).thenReturn(Optional.empty());

        assertThrows(SpecNotFoundException.class, () -> service.fetchAndSaveSpec(url));
    }
}