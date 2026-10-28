package com.bank.report.controller;

import com.bank.report.dto.ProductReportDto;
import com.bank.report.dto.TransactionDto;
import com.bank.report.service.ReportService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** REST endpoints for reports. */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/products/{productNumber}")
    public Single<ProductReportDto> productReport(
            @PathVariable String productNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.productReport(productNumber, from, to);
    }

    @GetMapping("/products/{productNumber}/last-movements")
    public Flowable<TransactionDto> lastMovements(@PathVariable String productNumber) {
        return reportService.lastMovements(productNumber);
    }
}