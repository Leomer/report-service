package com.bank.report.service.impl;

import com.bank.report.client.TransactionClient;
import com.bank.report.dto.BalanceDto;
import com.bank.report.dto.TransactionDto;
import com.bank.report.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for {@link ReportServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private TransactionClient transactionClient;

    @InjectMocks
    private ReportServiceImpl service;

    private final LocalDate from = LocalDate.now().minusDays(7);
    private final LocalDate to = LocalDate.now();

    private TransactionDto tx(String productType, String type, String amount, String fee) {
        TransactionDto t = new TransactionDto();
        t.setProductNumber("A1");
        t.setProductType(productType);
        t.setTransactionType(type);
        t.setAmount(new BigDecimal(amount));
        t.setFee(new BigDecimal(fee));
        return t;
    }

    private BalanceDto balance(String value) {
        BalanceDto b = new BalanceDto();
        b.setBalance(new BigDecimal(value));
        return b;
    }

    @Test
    void productReportShouldAggregateTotalsAndFees() {
        when(transactionClient.findBetween("A1", from, to)).thenReturn(Flux.just(
                tx("ACCOUNT", "DEPOSIT", "100", "0"),
                tx("ACCOUNT", "DEPOSIT", "50", "1.50"),
                tx("ACCOUNT", "WITHDRAWAL", "30", "0")));
        when(transactionClient.accountBalance("A1")).thenReturn(Mono.just(balance("118.50")));

        service.productReport("A1", from, to)
                .test()
                .assertValue(r -> r.getTotalMovements() == 3
                        && new BigDecimal("150").compareTo(r.getTotalsByType().get("DEPOSIT")) == 0
                        && new BigDecimal("30").compareTo(r.getTotalsByType().get("WITHDRAWAL")) == 0
                        && new BigDecimal("1.50").compareTo(r.getTotalFees()) == 0
                        && new BigDecimal("118.50").compareTo(r.getCurrentBalance()) == 0)
                .assertComplete();
    }

    @Test
    void productReportShouldUseCreditBalanceForCreditProducts() {
        when(transactionClient.findBetween("CR1", from, to))
                .thenReturn(Flux.just(tx("CREDIT", "CHARGE", "300", "0")));
        when(transactionClient.creditBalance("CR1")).thenReturn(Mono.just(balance("300")));

        service.productReport("CR1", from, to)
                .test()
                .assertValue(r -> "CREDIT".equals(r.getProductType()))
                .assertComplete();

        verify(transactionClient, never()).accountBalance(any());
    }

    @Test
    void productReportWithoutMovementsShouldFallBackToCreditBalance() {
        when(transactionClient.findBetween("CR1", from, to)).thenReturn(Flux.empty());
        when(transactionClient.accountBalance("CR1"))
                .thenReturn(Mono.error(new BusinessException("Account not found")));
        when(transactionClient.creditBalance("CR1")).thenReturn(Mono.just(balance("0")));

        service.productReport("CR1", from, to)
                .test()
                .assertValue(r -> r.getTotalMovements() == 0)
                .assertComplete();
    }

    @Test
    void productReportShouldFailWhenRangeIsInvalid() {
        service.productReport("A1", to, from).test().assertError(BusinessException.class);
        verify(transactionClient, never()).findBetween(any(), any(), any());
    }

    @Test
    void lastMovementsShouldDelegateToTransactionService() {
        when(transactionClient.findLast("CR1"))
                .thenReturn(Flux.just(tx("CREDIT", "CHARGE", "10", "0")));

        service.lastMovements("CR1").test().assertValueCount(1).assertComplete();
    }
}