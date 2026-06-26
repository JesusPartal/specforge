package com.jesuspartal.specforge.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesuspartal.specforge.domain.model.Spec;
import com.jesuspartal.specforge.exception.SpecNotFoundException;
import com.jesuspartal.specforge.infrastructure.repository.SpecRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostmanCollectionServiceTest {

    @Mock
    private SpecRepository specRepository;

    private PostmanCollectionService service;

    @BeforeEach
    void setUp() {
        service = new PostmanCollectionService(specRepository, new ObjectMapper());
    }

    @Test
    void shouldGenerateValidPostmanCollection() {
        Spec spec = Spec.builder()
                .id(1L).repoUrl("https://github.com/test/api").title("test/api")
                .version("1.0.0").rawContent(POSTMAN_SPEC).fetchedAt(LocalDateTime.now()).build();
        when(specRepository.findById(1L)).thenReturn(Optional.of(spec));

        var result = service.generateCollection(1L);

        assertTrue(result.has("info"));
        assertEquals("Test API", result.get("info").get("name").asText());
        assertEquals(1, result.get("item").size());
        assertEquals("GET", result.get("item").get(0).get("request").get("method").asText());
    }

    @Test
    void shouldThrowWhenSpecNotFound() {
        when(specRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(SpecNotFoundException.class, () -> service.generateCollection(99L));
    }

    private static final String POSTMAN_SPEC = """
            openapi: "3.0.0"
            info:
              title: Test API
              version: "1.0.0"
            paths:
              /pets:
                get:
                  summary: List all pets
                  responses:
                    '200':
                      description: OK
            """;
}