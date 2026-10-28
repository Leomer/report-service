package com.bank.report.service;

import com.bank.report.dto.ProductReportDto;
import com.bank.report.dto.TransactionDto;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

import java.time.LocalDate;

/** Read-only reports built from transaction-service data. */
public interface ReportService {
    /** General report of a product in a date range. */
    Single<ProductReportDto> productReport(String productNumber, LocalDate from, LocalDate to);
    /** Latest 10 movements of a product (credit card now, debit card in Part III). */
    Flowable<TransactionDto> lastMovements(String productNumber);
}