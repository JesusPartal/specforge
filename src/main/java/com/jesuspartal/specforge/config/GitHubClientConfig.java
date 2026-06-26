package com.jesuspartal.specforge.config;

import com.jesuspartal.specforge.application.service.OAuth2TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

@Configuration
public class GitHubClientConfig {

    @Bean
    public RestClient gitHubRestClient(OAuth2TokenService tokenService) {
        return RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .requestInterceptor(
                        new ClientHttpRequestInterceptor() {
                            @Override
                            public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                                                ClientHttpRequestExecution execution) throws java.io.IOException {
                                String token = tokenService.getAccessToken();
                                if (token != null) {
                                    request.getHeaders().setBearerAuth(token);
                                }
                                return execution.execute(request, body);
                            }
                        }
                )
                .build();
    }
}