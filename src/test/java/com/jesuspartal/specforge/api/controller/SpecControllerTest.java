package com.jesuspartal.specforge.api.controller;

import com.jesuspartal.specforge.api.dto.SpecResponse;
import com.jesuspartal.specforge.application.service.*;
import com.jesuspartal.specforge.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;



import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(SpecController.class)
@Import({SecurityConfig.class, SpecControllerTest.TestConfig.class})
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.github.client-id=test-client",
        "spring.security.oauth2.client.registration.github.client-secret=test-secret"
})
class SpecControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpecService specService;
    @MockitoBean
    private SpecParserService specParserService;
    @MockitoBean
    private PostmanCollectionService postmanCollectionService;
    @MockitoBean
    private TestSkeletonService testSkeletonService;
    @MockitoBean
    private OrgScanService orgScanService;
    @MockitoBean
    private SpecDiffService specDiffService;

    @Test
    @WithMockUser
    void shouldReturnAllSpecs() throws Exception {
        var spec = new SpecResponse(1L, "url", "title", "1.0", LocalDateTime.now());
        when(specService.getAllSpecs()).thenReturn(List.of(spec));

        mockMvc.perform(get("/api/specs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("title"));
    }

    @Test
    @WithMockUser
    void shouldReturn404ForNonExistentSpec() throws Exception {
        when(specParserService.summarize(99L))
                .thenThrow(new com.jesuspartal.specforge.exception.SpecNotFoundException(99L));

        mockMvc.perform(get("/api/specs/99/summary"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/specs"))
                .andExpect(status().is3xxRedirection());
    }
}