package com.bank.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** General report of a product within a date range. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReportDto {
    private String productNumber;
    private String productType;
    private LocalDate from;
    private LocalDate to;
    private BigDecimal currentBalance;
    private int totalMovements;
    private Map<String, BigDecimal> totalsByType;
    private BigDecimal totalFees;
    private List<TransactionDto> movements;
}