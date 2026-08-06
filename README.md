# Payment Service

A RESTful payment processing service built with Java 21 and Spring Boot 4. Demonstrates production-grade patterns: hexagonal architecture, idempotency, optimistic locking, audit trail, structured logging and JWT authentication.

---

## Tech Stack

| Layer          | Technology                          |
|----------------|-------------------------------------|
| Language       | Java 21                             |
| Framework      | Spring Boot 4.1                     |
| Security       | Spring Security + JWT (JJWT 0.12.6) |
| Database       | PostgreSQL 15                       |
| Migrations     | Flyway                              |
| Cache          | Redis 7                             |
| ORM            | Hibernate / Spring Data JPA         |
| Testing        | JUnit 5, Testcontainers             |
| Documentation  | SpringDoc OpenAPI 3                 |
| Build          | Maven                               |
| Infrastructure | Docker Compose                      |

---

## Architecture

The project follows **Hexagonal Architecture** (Ports & Adapters). The domain has no dependencies on Spring or JPA — it is plain Java.

```
src/main/java/com/example/payment_service/
├── domain/
│   ├── payment/           # Payment aggregate, PaymentStatus, PaymentRepository (interface)
│   ├── payment_history/   # PaymentHistory record, PaymentHistoryRepository (interface)
│   ├── user/              # User record, UserRole enum, UserRepository (interface)
│   ├── money/             # Money value object
│   └── exception/         # Domain exceptions
├── application/
│   └── service/           # PaymentService, AuditService, IdempotencyService, AuthService, JwtService
├── infrastructure/
│   ├── persistence/       # JPA entities, mappers, repository implementations
│   ├── security/          # JwtAuthenticationFilter, UserDetailsServiceImpl
│   ├── idempotency/       # @Idempotent annotation + IdempotencyAspect
│   ├── logging/           # LoggingAspect
│   └── config/            # SecurityConfig, RedisConfig, OpenApiConfig
└── api/
    ├── controller/        # PaymentController, AuthController
    ├── dto/               # PaymentCreationRequest, PaymentResponse, RegisterRequest, LoginRequest, AuthResponse
    └── exception_handler/ # GlobalExceptionHandler
```

### Dependency rule

```
api → application → domain ← infrastructure
```

`domain` knows nothing about Spring, JPA, Redis, or Security. `infrastructure` implements domain interfaces.

---

## Database Schema

### payments
| Column       | Type          | Notes                                     |
|--------------|---------------|-------------------------------------------|
| id           | UUID          | Primary key, generated in domain          |
| sender_id    | UUID          |                                           |
| recipient_id | UUID          |                                           |
| amount       | NUMERIC(19,4) |                                           |
| currency     | VARCHAR(3)    | ISO 4217 (USD, EUR, PLN)                  |
| status       | VARCHAR(20)   | PENDING / PROCESSING / COMPLETED / FAILED |
| version      | BIGINT        | Optimistic locking                        |
| created_at   | TIMESTAMP     |                                           |

### payment_history
| Column      | Type        | Notes             |
|-------------|-------------|-------------------|
| id          | UUID        | Primary key       |
| payment_id  | UUID        | FK → payments(id) |
| old_status  | VARCHAR(20) | NULL on creation  |
| new_status  | VARCHAR(20) |                   |
| changed_at  | TIMESTAMP   |                   |

### users
| Column    | Type          | Notes            |
|-----------|---------------|------------------|
| id        | UUID          | Primary key      |
| username  | VARCHAR(50)   | Unique           |
| password  | VARCHAR(60)   | BCrypt hash      |
| email     | VARCHAR(100)  | Unique           |
| user_role | VARCHAR(10)   | MERCHANT / ADMIN |

---

## Payment State Machine

```
PENDING → PROCESSING → COMPLETED
                    ↘ FAILED
```

Status transitions are validated inside the `Payment` aggregate. Calling `complete()` on a `PENDING` payment throws `InvalidPaymentStatusException` (409 Conflict).

---

## Running Locally

### Prerequisites
- Docker Desktop
- Java 21
- Maven

### Environment variables

Copy `.env.example` to `.env` and fill in the values:

```bash
cp .env.example .env
```

### Start infrastructure

```bash
docker-compose up -d
```

This starts PostgreSQL on port `5432` and Redis on port `6379`.

### Run the application

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080` with profile `dev`.

### Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

## API Examples

### Authentication

All payment endpoints require a JWT token. First register and obtain a token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "usernameExample",
    "password": "passwordExample",
    "email": "example@example.com"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9..."
}
```

Login with existing credentials:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "usernameExample",
    "password": "passwordExample"
  }'
```

Use the token in subsequent requests via `Authorization: Bearer <token>` header.

---

### Create a payment

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Idempotency-Key: unique-key-123" \
  -d '{
    "senderId": "UUID_1",
    "recipientId": "UUID_2",
    "amount": 100.00,
    "currency": "USD"
  }'
```

Response:
```json
{
  "id": "UUID_1",
  "senderId": "UUID_2",
  "recipientId": "UUID_3",
  "amount": 100.00,
  "currency": "USD",
  "status": "PENDING",
  "createdAt": "2026-01-01T12:00:00"
}
```

### Get a payment

```bash
curl http://localhost:8080/api/v1/payments/{id} \
  -H "Authorization: Bearer <token>"
```

### Process a payment

```bash
curl -X PATCH http://localhost:8080/api/v1/payments/{id}/process \
  -H "Authorization: Bearer <token>"
```

### Complete a payment

```bash
curl -X PATCH http://localhost:8080/api/v1/payments/{id}/complete \
  -H "Authorization: Bearer <token>"
```

### Fail a payment

```bash
curl -X PATCH http://localhost:8080/api/v1/payments/{id}/fail \
  -H "Authorization: Bearer <token>"
```

---

## Error Responses

All errors follow RFC 7807 Problem Details:

| Status | Title                   | When                                              |
|--------|-------------------------|---------------------------------------------------|
| 400    | Validation failed       | Invalid request body                              |
| 400    | Missing required header | `Idempotency-Key` not provided                    |
| 401    | Unauthorized            | Missing or invalid JWT token / incorrect password |
| 404    | Payment not found       | Payment ID does not exist                         |
| 409    | Invalid payment status  | Illegal state transition                          |

---

## Key Architectural Decisions

### JWT Authentication
All endpoints except `/api/v1/auth/**` and Swagger UI require a valid JWT token. Tokens are signed with HMAC-SHA384, contain the username and role in the payload, and expire after 24 hours. The `JwtAuthenticationFilter` validates the token on every request before it reaches the controller.

### Idempotency
Every `POST /payments` request requires an `Idempotency-Key` header. The key and its response are stored in Redis with a 24-hour TTL. Duplicate requests return the cached response without creating a new payment. Implemented as a reusable `@Idempotent` AOP annotation.

### Hexagonal Architecture
The `Payment` domain class is a plain Java object with no framework annotations. It contains all business logic including state machine validation. JPA mapping lives in a separate `PaymentJpaEntity` in the infrastructure layer. Domain logic can be unit-tested without starting Spring or a database.

### Optimistic Locking
`PaymentJpaEntity` uses `@Version` to prevent lost updates under concurrent requests. If two requests attempt to update the same payment simultaneously, the second one receives an `OptimisticLockException`.

### Audit Trail
Every status change is recorded in `payment_history` within the same transaction as the payment update. This provides a complete, traceable history of each payment's lifecycle.

### AOP Logging
Method entry, exit, and execution time are logged via `LoggingAspect` without polluting business logic. Exception handlers are logged separately with a concise message rather than a full stack trace. Log output is split by profile: application logs go to both console and file, Spring framework logs go to console only.

---

## Running Tests

```bash
mvn test
```

Tests use Testcontainers — Docker must be running. PostgreSQL and Redis containers start automatically and are shared across all test classes.

### Test coverage
- **Unit tests** — `Payment`, `Money` domain logic; `JwtService` token generation and validation; `AuthService` with Mockito mocks
- **Integration tests** — repository layer with real PostgreSQL
- **End-to-end tests** — full HTTP stack with `MockMvc`: payment lifecycle, status transitions, idempotency, authentication
