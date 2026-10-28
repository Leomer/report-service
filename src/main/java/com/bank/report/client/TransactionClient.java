package com.bank.report.client;

import com.bank.report.dto.BalanceDto;
import com.bank.report.dto.TransactionDto;
import com.bank.report.exception.BusinessException;
import com.bank.report.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Map;

/** Calls transaction-service, protected by a circuit breaker with a 2 s timeout. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionClient {

    private static final String CIRCUIT = "transaction-service";

    private final WebClient transactionWebClient;
    private final ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;

    /** Movements of a product between two dates (inclusive). */
    public Flux<TransactionDto> findBetween(String productNumber, LocalDate from, LocalDate to) {
        Flux<TransactionDto> call = transactionWebClient.get()
                .uri(u -> u.path("/transactions/product/{n}/between")
                        .queryParam("from", from).queryParam("to", to).build(productNumber))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::toBusinessException)
                .bodyToFlux(TransactionDto.class);
        return circuitBreakerFactory.create(CIRCUIT).run(call, this::fluxFallback);
    }

    /** Latest 10 movements of a product. */
    public Flux<TransactionDto> findLast(String productNumber) {
        Flux<TransactionDto> call = transactionWebClient.get()
                .uri("/transactions/product/{n}/last", productNumber)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::toBusinessException)
                .bodyToFlux(TransactionDto.class);
        return circuitBreakerFactory.create(CIRCUIT).run(call, this::fluxFallback);
    }

    /** Balance of an account. */
    public Mono<BalanceDto> accountBalance(String accountNumber) {
        return balance("/transactions/balance/accounts/{n}", accountNumber);
    }

    /** Balance of a credit product. */
    public Mono<BalanceDto> creditBalance(String creditNumber) {
        return balance("/transactions/balance/credits/{n}", creditNumber);
    }

    private Mono<BalanceDto> balance(String path, String number) {
        Mono<BalanceDto> call = transactionWebClient.get()
                .uri(path, number)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::toBusinessException)
                .bodyToMono(BalanceDto.class);
        return circuitBreakerFactory.create(CIRCUIT).run(call, this::monoFallback);
    }

    private Mono<BusinessException> toBusinessException(ClientResponse response) {
        return response.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() { })
                .map(body -> new BusinessException(body.getOrDefault("error", "Request rejected")))
                .defaultIfEmpty(new BusinessException("Request rejected"));
    }

    private Flux<TransactionDto> fluxFallback(Throwable ex) {
        if (ex instanceof BusinessException) {
            return Flux.error(ex);
        }
        log.warn("transaction-service fallback triggered: {}", ex.toString());
        return Flux.error(new ServiceUnavailableException(
                "transaction-service is unavailable, try again later"));
    }

    private Mono<BalanceDto> monoFallback(Throwable ex) {
        if (ex instanceof BusinessException) {
            return Mono.error(ex);
        }
        log.warn("transaction-service fallback triggered: {}", ex.toString());
        return Mono.error(new ServiceUnavailableException(
                "transaction-service is unavailable, try again later"));
    }
}