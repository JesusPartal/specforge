package com.jesuspartal.specforge.application.service;

import com.jesuspartal.specforge.domain.model.Spec;
import com.jesuspartal.specforge.exception.SpecNotFoundException;
import com.jesuspartal.specforge.infrastructure.repository.SpecRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecParserServiceTest {

    @Mock
    private SpecRepository specRepository;

    @InjectMocks
    private SpecParserService service;

    @Test
    void shouldSummarizeValidSpec() {
        Spec spec = Spec.builder()
                .id(1L).repoUrl("https://github.com/test/api").title("test/api")
                .version("1.0.0").rawContent(MINIMAL_SPEC).fetchedAt(LocalDateTime.now()).build();
        when(specRepository.findById(1L)).thenReturn(Optional.of(spec));

        var result = service.summarize(1L);

        assertEquals("Test API", result.title());
        assertEquals("1.0.0", result.version());
        assertEquals(2, result.endpoints().size());
    }

    @Test
    void shouldThrowWhenSpecNotFound() {
        when(specRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(SpecNotFoundException.class, () -> service.summarize(99L));
    }

    private static final String MINIMAL_SPEC = """
            openapi: "3.0.0"
            info:
              title: Test API
              version: "1.0.0"
              description: A test API
            paths:
              /pets:
                get:
                  summary: List all pets
                  responses:
                    '200':
                      description: OK
                post:
                  summary: Create a pet
                  responses:
                    '201':
                      description: Created
            """;
}