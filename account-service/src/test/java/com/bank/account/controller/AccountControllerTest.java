package com.bank.account.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.UpdateBalanceRequest;
import com.bank.account.entity.AccountStatus;
import com.bank.account.entity.AccountType;
import com.bank.account.service.AccountService;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "internal.api-token=test-internal-token")
@DisplayName("AccountController Internal Endpoint Tests")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    @DisplayName("internal account lookup should return account response")
    void testInternalAccountLookup() throws Exception {
        AccountResponse response = AccountResponse.builder()
                .id(10L)
                .accountNumber("ACC123")
                .userId(42L)
                .accountHolderName("Jane Doe")
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .balance(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(accountService.getAccountByAccountNumber("ACC123")).thenReturn(response);

        mockMvc.perform(get("/api/accounts/internal/ACC123")
            .header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountNumber").value("ACC123"))
            .andExpect(jsonPath("$.userId").value(42));
    }

    @Test
    @DisplayName("internal account lookup should reject invalid token")
    void testInternalAccountLookupInvalidToken() throws Exception {
        mockMvc.perform(get("/api/accounts/internal/ACC123")
                .header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());

        org.mockito.Mockito.verify(accountService, never()).getAccountByAccountNumber("ACC123");
    }

    @Test
    @DisplayName("internal debit should process with valid token")
    void testInternalDebit() throws Exception {
        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.setAccountNumber("ACC123");
        request.setAmount(new BigDecimal("10.00"));

        mockMvc.perform(post("/api/accounts/internal/debit")
                .header("X-Internal-Token", "test-internal-token")
                .contentType("application/json")
                .content("{\"accountNumber\":\"ACC123\",\"amount\":10.00}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("internal credit should process with valid token")
    void testInternalCredit() throws Exception {
        mockMvc.perform(post("/api/accounts/internal/credit")
                .header("X-Internal-Token", "test-internal-token")
                .contentType("application/json")
                .content("{\"accountNumber\":\"ACC123\",\"amount\":10.00}"))
                .andExpect(status().isOk());
    }
}
