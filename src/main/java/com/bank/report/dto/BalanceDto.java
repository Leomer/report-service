package com.bank.report.dto;

import lombok.Data;

import java.math.BigDecimal;

/** Balance as returned by transaction-service. */
@Data
public class BalanceDto {
    private String productNumber;
    private String productType;
    private BigDecimal balance;
    private BigDecimal available;
}