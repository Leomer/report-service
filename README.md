# report-service

Reporting microservice. Builds read-only reports from the movement history
served by transaction-service. Spring Boot, WebFlux, RxJava 3, Resilience4j.

## Design decisions
- **No database.** It aggregates data and computes totals with Java streams,
  so it respects the database-per-service pattern: it never touches
  `transaction_db` or any other service's data.
- **Single dependency:** transaction-service, called through REST with a
  circuit breaker and a 2 s timeout (Resilience4j).
- **REST is allowed here** because this service belongs to Part II.
  In Part III it could be fed with Kafka events instead.

## Requirements
- JDK 17, Maven
- config-server (:8888), eureka-server (:8761)
- transaction-service (:8084) and its dependencies (account, credit)

## Run
1. Start config-server, eureka-server, the business services and transaction-service
2. `mvn spring-boot:run`
- Port: 8085 (from config-repo)
- Swagger UI: http://localhost:8085/swagger-ui.html
- Through the gateway: http://localhost:8080/reports/...

## Endpoints
| Method | Path | Description |
|---|---|---|
| GET | /reports/products/{productNumber}?from=YYYY-MM-DD&to=YYYY-MM-DD | General report of a product in a date range |
| GET | /reports/products/{productNumber}/last-movements | Latest 10 movements of a product |

### General report response
- `currentBalance`: current balance (debt for credit products)
- `totalMovements`: number of movements in the range
- `totalsByType`: sum of amounts per transaction type
- `totalFees`: commissions charged in the range
- `movements`: detailed list, newest first

## Error handling
| Status | Cause |
|---|---|
| 400 | Invalid date range (`from` after `to`) or unknown product |
| 503 | transaction-service unavailable, too slow (> 2 s) or circuit open |

## Documentation
- OpenAPI contract: `docs/openapi.yaml`
- Sequence diagram: `docs/sequence-report.puml`
- Architecture diagram: see the `bank-docs` repository

## Known limitations
- If the date range has no movements, the product type is unknown, so the
  service tries the account balance first and then the credit balance.
- The "last 10 movements" report works for credit cards now; it will cover
  debit cards when that product exists (Part III), since it filters by
  product number.
- Totals are computed in memory; for very large ranges a database
  aggregation or pagination would be needed.

## Tests
`mvn clean test`. Coverage report: `target/site/jacoco/index.html`.