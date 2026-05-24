package com.bank.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.bank.transaction.dto.UpdateBalanceRequest;

@FeignClient(name = "account-service", url = "${account.service.url}")
public interface AccountClient {

    @PostMapping("/api/accounts/internal/debit")
    void debit(@RequestHeader("X-Internal-Token") String internalToken, @RequestBody UpdateBalanceRequest request);

    @PostMapping("/api/accounts/internal/credit")
    void credit(@RequestHeader("X-Internal-Token") String internalToken, @RequestBody UpdateBalanceRequest request);
}
