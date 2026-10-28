package com.bank.report.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Movement as returned by transaction-service. */
@Data
public class TransactionDto {
    private String id;
    private String productNumber;
    private String relatedProductNumber;
    private String productType;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal balanceAfter;
    private LocalDateTime date;
}