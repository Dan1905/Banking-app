package com.bank.transaction.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bank.transaction.client.AccountClient;
import com.bank.transaction.client.AuthClient;
import com.bank.transaction.dto.AccountLookupResponse;
import com.bank.transaction.dto.DepositWithdrawalRequest;
import com.bank.transaction.dto.TransactionResponse;
import com.bank.transaction.dto.TransferRequest;
import com.bank.transaction.dto.UpdateBalanceRequest;
import com.bank.transaction.dto.UserEmailResponse;
import com.bank.transaction.entity.Transaction;
import com.bank.transaction.entity.TransactionStatus;
import com.bank.transaction.entity.TransactionType;
import com.bank.transaction.exception.TransactionNotFoundException;
import com.bank.transaction.kafka.TransactionEvent;
import com.bank.transaction.kafka.TransactionProducer;
import com.bank.transaction.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountClient accountClient;

    @Mock
    private AuthClient authClient;

    @Mock
    private TransactionProducer transactionProducer;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction transaction;

    private void prepare() {
        ReflectionTestUtils.setField(transactionService, "internalToken", "internal-token");

        transaction = Transaction.builder()
                .id(1L)
                .transactionId("txn-123")
                .fromAccountNumber("ACC100")
                .toAccountNumber("ACC200")
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .description("test")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("transfer should debit, credit, persist, and publish event")
    void testTransferSuccess() throws Exception {
        prepare();
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            if (saved.getTransactionId() == null) {
                saved.setTransactionId("txn-123");
            }
            if (saved.getCreatedAt() == null) {
                saved.setCreatedAt(LocalDateTime.now());
            }
            return saved;
        });

        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("ACC100");
        request.setToAccountNumber("ACC200");
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("rent");

        TransactionResponse response = transactionService.transfer(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        verify(accountClient, times(1)).debit(any(String.class), any(UpdateBalanceRequest.class));
        verify(accountClient, times(1)).credit(any(String.class), any(UpdateBalanceRequest.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(transactionProducer, times(1)).publishTransactionEvent(any());
    }

    @Test
    @DisplayName("transfer should populate email from internal lookups")
    void testTransferPopulatesEmail() throws Exception {
        prepare();
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            if (saved.getTransactionId() == null) {
                saved.setTransactionId("txn-456");
            }
            if (saved.getCreatedAt() == null) {
                saved.setCreatedAt(LocalDateTime.now());
            }
            return saved;
        });

        AccountLookupResponse account = new AccountLookupResponse();
        account.setUserId(42L);
        when(accountClient.getAccountInternal(eq("internal-token"), eq("ACC100")))
                .thenReturn(account);

        UserEmailResponse user = new UserEmailResponse();
        user.setId(42L);
        user.setEmail("user@example.com");
        when(authClient.getUserEmail(eq("internal-token"), eq(42L))).thenReturn(user);

        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("ACC100");
        request.setToAccountNumber("ACC200");
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("rent");

        transactionService.transfer(request);

        ArgumentCaptor<TransactionEvent> eventCaptor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(transactionProducer, times(1)).publishTransactionEvent(eventCaptor.capture());
        assertEquals("user@example.com", eventCaptor.getValue().getEmail());
    }

    @Test
    @DisplayName("transfer should reject same source and destination account")
    void testTransferSameAccountRejected() {
        prepare();
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("ACC100");
        request.setToAccountNumber("ACC100");
        request.setAmount(new BigDecimal("100.00"));

        jakarta.transaction.InvalidTransactionException ex = assertThrows(jakarta.transaction.InvalidTransactionException.class,
            () -> transactionService.transfer(request));
        assertNotNull(ex);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("depositOrWithdraw should process deposit and publish event")
    void testDepositSuccess() throws Exception {
        prepare();
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            if (saved.getTransactionId() == null) {
                saved.setTransactionId("txn-deposit");
            }
            if (saved.getCreatedAt() == null) {
                saved.setCreatedAt(LocalDateTime.now());
            }
            return saved;
        });

        DepositWithdrawalRequest request = new DepositWithdrawalRequest();
        request.setAccountNumber("ACC200");
        request.setAmount(new BigDecimal("50.00"));
        request.setType(TransactionType.DEPOSIT);
        request.setDescription("top up");

        TransactionResponse response = transactionService.depositOrWithdraw(request);

        assertNotNull(response);
        assertEquals(TransactionType.DEPOSIT, response.getType());
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        verify(accountClient, times(1)).credit(any(String.class), any(UpdateBalanceRequest.class));
        verify(transactionProducer, times(1)).publishTransactionEvent(any());
    }

    @Test
    @DisplayName("depositOrWithdraw should process withdrawal")
    void testWithdrawalSuccess() throws Exception {
        prepare();
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            if (saved.getTransactionId() == null) {
                saved.setTransactionId("txn-withdrawal");
            }
            if (saved.getCreatedAt() == null) {
                saved.setCreatedAt(LocalDateTime.now());
            }
            return saved;
        });

        DepositWithdrawalRequest request = new DepositWithdrawalRequest();
        request.setAccountNumber("ACC100");
        request.setAmount(new BigDecimal("25.00"));
        request.setType(TransactionType.WITHDRAWAL);
        request.setDescription("cash out");

        TransactionResponse response = transactionService.depositOrWithdraw(request);

        assertNotNull(response);
        assertEquals(TransactionType.WITHDRAWAL, response.getType());
        verify(accountClient, times(1)).debit(any(String.class), any(UpdateBalanceRequest.class));
    }

    @Test
    @DisplayName("depositOrWithdraw should reject transfer type")
    void testDepositWithdrawRejectTransferType() {
        prepare();
        DepositWithdrawalRequest request = new DepositWithdrawalRequest();
        request.setAccountNumber("ACC100");
        request.setAmount(new BigDecimal("25.00"));
        request.setType(TransactionType.TRANSFER);

        jakarta.transaction.InvalidTransactionException ex = assertThrows(jakarta.transaction.InvalidTransactionException.class,
            () -> transactionService.depositOrWithdraw(request));
        assertNotNull(ex);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("getTransaction should return transaction response")
    void testGetTransaction() {
        prepare();
        when(transactionRepository.findByTransactionId("txn-123")).thenReturn(Optional.of(transaction));

        TransactionResponse response = transactionService.getTransaction("txn-123");

        assertEquals("txn-123", response.getTransactionId());
    }

    @Test
    @DisplayName("getTransaction should throw when missing")
    void testGetTransactionMissing() {
        prepare();
        when(transactionRepository.findByTransactionId("missing")).thenReturn(Optional.empty());

        TransactionNotFoundException ex = assertThrows(TransactionNotFoundException.class,
                () -> transactionService.getTransaction("missing"));
        assertNotNull(ex);
    }

    @Test
    @DisplayName("getAccountHistory should map repository results")
    void testGetAccountHistory() {
        prepare();
        when(transactionRepository.findByFromAccountNumberOrToAccountNumber("ACC100", "ACC100"))
                .thenReturn(List.of(transaction));

        List<TransactionResponse> history = transactionService.getAccountHistory("ACC100");

        assertEquals(1, history.size());
        assertEquals("txn-123", history.get(0).getTransactionId());
    }
}
