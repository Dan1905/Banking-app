package com.bank.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import com.bank.api_gateway.filter.JwtAuthFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {
    private final JwtAuthFilter jwtAuthFilter;

    @Value("${services.auth-url}")
    private String authUrl;

    @Value("${services.account-url}")
    private String accountUrl;

    @Value("${services.transaction-url}")
    private String transactionUrl;

   @Bean
    public RouterFunction<ServerResponse> authRoutes() {
        return GatewayRouterFunctions.route("auth-service")
                .route(RequestPredicates.path("/api/auth/**"),
                        HandlerFunctions.http())
                .filter((request, next) -> {
                    jwtAuthFilter.filter(request);
                    return next.handle(request);
                })
            .before(uri(authUrl))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> accountRoutes() {
        return GatewayRouterFunctions.route("account-service")
                .route(RequestPredicates.path("/api/accounts/**"),
                        HandlerFunctions.http())
                .filter((request, next) -> {
                    jwtAuthFilter.filter(request);
                    return next.handle(request);
                })
            .before(uri(accountUrl))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> transactionRoutes() {
        return GatewayRouterFunctions.route("transaction-service")
                .route(RequestPredicates.path("/api/transactions/**"),
                        HandlerFunctions.http())
                .filter((request, next) -> {
                    jwtAuthFilter.filter(request);
                    return next.handle(request);
                })
            .before(uri(transactionUrl))
                .build();
    }
}
