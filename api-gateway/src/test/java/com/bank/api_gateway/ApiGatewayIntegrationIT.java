package com.bank.api_gateway;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.function.RouterFunction;

@SpringBootTest
class ApiGatewayIntegrationIT {

    @Autowired
    private ApplicationContext context;

    @Test
    void gatewayRouteBeansPresent() {
        assertTrue(context.containsBean("authRoutes"));
        assertTrue(context.containsBean("accountRoutes"));
        assertTrue(context.containsBean("transactionRoutes"));

        Object auth = context.getBean("authRoutes");
        Object account = context.getBean("accountRoutes");
        Object txn = context.getBean("transactionRoutes");

        assertNotNull(auth);
        assertNotNull(account);
        assertNotNull(txn);

        // verify types
        assertTrue(auth instanceof RouterFunction);
        assertTrue(account instanceof RouterFunction);
        assertTrue(txn instanceof RouterFunction);
    }

}
