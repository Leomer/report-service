package com.bank.report.service.impl;

import com.bank.report.client.TransactionClient;
import com.bank.report.dto.BalanceDto;
import com.bank.report.dto.ProductReportDto;
import com.bank.report.dto.TransactionDto;
import com.bank.report.exception.BusinessException;
import com.bank.report.service.ReportService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Default implementation of {@link ReportService}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final TransactionClient transactionClient;

    @Override
    public Single<ProductReportDto> productReport(String productNumber, LocalDate from, LocalDate to) {
        log.info("Report of {} from {} to {}", productNumber, from, to);
        if (from == null || to == null || to.isBefore(from)) {
            return Single.error(new BusinessException("Invalid date range"));
        }
        return Flowable.fromPublisher(transactionClient.findBetween(productNumber, from, to))
                .toList()
                .flatMap(list -> currentBalance(productNumber, list)
                        .map(balance -> buildReport(productNumber, from, to, list, balance)));
    }

    @Override
    public Flowable<TransactionDto> lastMovements(String productNumber) {
        return Flowable.fromPublisher(transactionClient.findLast(productNumber));
    }

    /** Uses the movements to know the product kind; with no movements, tries account then credit. */
    private Single<BigDecimal> currentBalance(String productNumber, List<TransactionDto> movements) {
        if (!movements.isEmpty()) {
            boolean credit = "CREDIT".equals(movements.get(0).getProductType());
            return Single.fromPublisher(credit
                            ? transactionClient.creditBalance(productNumber)
                            : transactionClient.accountBalance(productNumber))
                    .map(BalanceDto::getBalance);
        }
        return Single.fromPublisher(transactionClient.accountBalance(productNumber))
                .onErrorResumeNext(e -> e instanceof BusinessException
                        ? Single.fromPublisher(transactionClient.creditBalance(productNumber))
                        : Single.error(e))
                .map(BalanceDto::getBalance);
    }

    /** Aggregates the movements with streams. */
    private ProductReportDto buildReport(String productNumber, LocalDate from, LocalDate to,
                                         List<TransactionDto> list, BigDecimal balance) {
        Map<String, BigDecimal> totals = list.stream().collect(Collectors.groupingBy(
                TransactionDto::getTransactionType,
                Collectors.reducing(BigDecimal.ZERO, TransactionDto::getAmount, BigDecimal::add)));
        BigDecimal fees = list.stream()
                .map(t -> t.getFee() == null ? BigDecimal.ZERO : t.getFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ProductReportDto.builder()
                .productNumber(productNumber)
                .productType(list.isEmpty() ? null : list.get(0).getProductType())
                .from(from).to(to)
                .currentBalance(balance)
                .totalMovements(list.size())
                .totalsByType(totals)
                .totalFees(fees)
                .movements(list)
                .build();
    }
}