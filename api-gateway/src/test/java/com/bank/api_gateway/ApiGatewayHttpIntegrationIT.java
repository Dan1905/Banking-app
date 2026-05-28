package com.bank.api_gateway;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;

import com.bank.api_gateway.filter.JwtAuthFilter;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayHttpIntegrationIT {

    private static MockWebServer mockWebServer;

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @BeforeAll
    @SuppressWarnings("unused")
    static void startMockServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().endsWith("/downstream/hello")) {
                    return new MockResponse().setResponseCode(200).setBody("hello-from-downstream")
                            .addHeader("Content-Type", "text/plain");
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        mockWebServer.start();
    }

    

    @DynamicPropertySource
    @SuppressWarnings("unused")
    static void registerProperties(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + mockWebServer.getPort();
        registry.add("services.auth-url", () -> url);
        registry.add("services.account-url", () -> url);
        registry.add("services.transaction-url", () -> url);
    }

    @Test
    void proxyRequestToDownstream() {
        // Gateway is configured to forward /api/auth/** to services.auth-url
        String gatewayUrl = "http://localhost:" + port + "/api/auth/downstream/hello";
        var resp = restTemplate.getForEntity(gatewayUrl, String.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).contains("hello-from-downstream");
    }

    @TestConfiguration
    @SuppressWarnings("unused")
    static class NoAuthConfig {
        @Bean
        public JwtAuthFilter jwtAuthFilter() {
            return new JwtAuthFilter() {
                @Override
                public void filter(org.springframework.web.servlet.function.ServerRequest request) {
                    // no-op for tests
                }
            };
        }
    }
}
