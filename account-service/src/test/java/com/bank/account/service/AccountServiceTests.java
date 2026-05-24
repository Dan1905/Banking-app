package com.bank.account.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.account.dto.CreateAccountRequest;
import com.bank.account.dto.UpdateBalanceRequest;
import com.bank.account.entity.Account;
import com.bank.account.entity.AccountStatus;
import com.bank.account.entity.AccountType;
import com.bank.account.exception.AccountNotFoundException;
import com.bank.account.exception.InsufficientFundsException;
import com.bank.account.exception.InvalidAccountOperationException;
import com.bank.account.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTests {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        sampleAccount = Account.builder()
                .id(1L)
                .accountNumber("ACC123456789")
                .userId(42L)
                .accountHolderName("Jane Doe")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("100.00"))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createAccount should persist a new account and return response")
    void testCreateAccount() {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });

        CreateAccountRequest req = new CreateAccountRequest();
        req.setUserId(42L);
        req.setAccountHolderName("Jane Doe");
        req.setAccountType(AccountType.SAVINGS);

        var resp = accountService.createAccount(req);

        assertNotNull(resp);
        assertEquals(99L, resp.getId());
        assertEquals(req.getUserId(), resp.getUserId());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("getAccountByAccountNumber should return mapped response when active")
    void testGetAccountByAccountNumber() {
        when(accountRepository.findByAccountNumber(sampleAccount.getAccountNumber()))
                .thenReturn(Optional.of(sampleAccount));

        var resp = accountService.getAccountByAccountNumber(sampleAccount.getAccountNumber());

        assertNotNull(resp);
        assertEquals(sampleAccount.getAccountNumber(), resp.getAccountNumber());
    }

    @Test
    @DisplayName("getAccountsByUserId should map repository results")
    void testGetAccountsByUserId() {
        when(accountRepository.findByUserId(42L)).thenReturn(List.of(sampleAccount));

        var list = accountService.getAccountsByUserId(42L);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(sampleAccount.getAccountNumber(), list.get(0).getAccountNumber());
    }

    @Test
    @DisplayName("getBalance should return account balance for active account")
    void testGetBalance() {
        when(accountRepository.findByAccountNumber(sampleAccount.getAccountNumber()))
                .thenReturn(Optional.of(sampleAccount));

        var bal = accountService.getBalance(sampleAccount.getAccountNumber());

        assertNotNull(bal);
        assertEquals(sampleAccount.getBalance(), bal.getBalance());
    }

    @Test
    @DisplayName("debit should throw when insufficient funds")
    void testDebitInsufficientFunds() {
        when(accountRepository.findByAccountNumber(sampleAccount.getAccountNumber()))
                .thenReturn(Optional.of(sampleAccount));

        UpdateBalanceRequest req = new UpdateBalanceRequest();
        req.setAccountNumber(sampleAccount.getAccountNumber());
        req.setAmount(new BigDecimal("1000.00"));

        assertThrows(InsufficientFundsException.class, () -> accountService.debit(req));
    }

    @Test
    @DisplayName("debit should subtract amount and save when sufficient funds")
    void testDebitSuccess() {
        when(accountRepository.findByAccountNumber(sampleAccount.getAccountNumber()))
                .thenReturn(Optional.of(sampleAccount));

        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateBalanceRequest req = new UpdateBalanceRequest();
        req.setAccountNumber(sampleAccount.getAccountNumber());
        req.setAmount(new BigDecimal("50.00"));

        accountService.debit(req);

        verify(accountRepository, times(1)).save(any(Account.class));
        assertEquals(new BigDecimal("50.00"), sampleAccount.getBalance());
    }

    @Test
    @DisplayName("credit should add amount and save")
    void testCredit() {
        when(accountRepository.findByAccountNumber(sampleAccount.getAccountNumber()))
                .thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateBalanceRequest req = new UpdateBalanceRequest();
        req.setAccountNumber(sampleAccount.getAccountNumber());
        req.setAmount(new BigDecimal("30.00"));

        accountService.credit(req);

        verify(accountRepository, times(1)).save(any(Account.class));
        assertEquals(new BigDecimal("130.00"), sampleAccount.getBalance());
    }

    @Test
    @DisplayName("updateAccountStatus should change status when account exists")
    void testUpdateAccountStatus() {
        when(accountRepository.findByAccountNumber(sampleAccount.getAccountNumber()))
                .thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = accountService.updateAccountStatus(sampleAccount.getAccountNumber(), AccountStatus.FROZEN);

        assertEquals(AccountStatus.FROZEN, resp.getStatus());
    }

    @Test
    @DisplayName("closeAccount should throw when balance non-zero")
    void testCloseAccountWithNonZeroBalance() {
        when(accountRepository.findByAccountNumber(sampleAccount.getAccountNumber()))
                .thenReturn(Optional.of(sampleAccount));

        assertThrows(InvalidAccountOperationException.class, () -> accountService.closeAccount(sampleAccount.getAccountNumber()));
    }

    @Test
    @DisplayName("closeAccount should set status to CLOSED when balance zero")
    void testCloseAccountSuccess() {
        sampleAccount.setBalance(BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber(sampleAccount.getAccountNumber()))
                .thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.closeAccount(sampleAccount.getAccountNumber());

        assertEquals(AccountStatus.CLOSED, sampleAccount.getStatus());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("getAccountByAccountNumber should throw AccountNotFoundException when missing")
    void testGetAccountNotFound() {
        when(accountRepository.findByAccountNumber("nope")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getAccountByAccountNumber("nope"));
    }
}
