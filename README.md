# Payment Service

A RESTful payment processing service built with Java 21 and Spring Boot 4. Demonstrates production-grade patterns: hexagonal architecture, idempotency, optimistic locking, audit trail, structured logging, JWT authentication, and a real payment service provider (Stripe) integration with signed webhooks.

---

## Tech Stack

| Layer          | Technology                          |
|----------------|-------------------------------------|
| Language       | Java 21                             |
| Framework      | Spring Boot 4.1                     |
| Security       | Spring Security + JWT (JJWT 0.12.6) |
| Payments (PSP) | Stripe Java SDK                     |
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

The project follows **Hexagonal Architecture** (Ports & Adapters). The domain has no dependencies on Spring, JPA, or Stripe - it is plain Java.

```
src/main/java/com/example/payment_service/
├── domain/
│   ├── payment/                # Payment aggregate, PaymentStatus, PaymentRepository (interface)
│   ├── payment_history/        # PaymentHistory record, PaymentHistoryRepository (interface)
│   ├── user/                   # User record, UserRole enum, UserRepository (interface)
│   ├── money/                  # Money value object
│   └── exception/              # Domain exceptions
├── application/
│   └── service/                # Service classes
│       ├── PspClient           # Port - abstracts the payment service provider
│       └── dto/                # PspPaymentResult, PaymentCreationResult
├── infrastructure/
│   ├── persistence/            # JPA entities, mappers, repository implementations
│   ├── security/               # JwtAuthenticationFilter, UserDetailsServiceImpl, UserPrincipal
│   ├── idempotency/            # @Idempotent annotation + IdempotencyAspect
│   ├── initializer             # AdminInitializer
│   ├── logging/                # LoggingAspect
│   ├── psp/                    # StripeClientImpl (PspClient adapter), StripeEventTypes util class
│   └── config/                 # SecurityConfig, RedisConfig, StripeConfig
└── api/
    ├── controller/             # PaymentController, AuthController, StripeWebhookController
    ├── dto/                    # PaymentCreationRequest, PaymentResponse, RegisterRequest, LoginRequest, AuthResponse
    └── exception_handler/      # GlobalExceptionHandler
```

### Dependency rule

```
api → application → domain ← infrastructure
```

`domain` knows nothing about Spring, JPA, Redis, Security, or Stripe. `infrastructure` implements domain interfaces and application ports (`PspClient`). Stripe is treated as one possible adapter behind `PspClient` - swapping providers means writing a new adapter, not touching `PaymentService` or the domain.

---

## Database Schema

### payments
| Column                      | Type          | Notes                                                               |
|-----------------------------|---------------|---------------------------------------------------------------------|
| id                          | UUID          | Primary key, generated in domain                                    |
| sender_id                   | UUID          | Derived from the authenticated JWT - never accepted from the client |
| recipient_id                | UUID          |                                                                     |
| amount                      | NUMERIC(19,4) |                                                                     |
| currency                    | VARCHAR(3)    | ISO 4217 (USD, EUR, PLN)                                            |
| status                      | VARCHAR(20)   | PENDING / COMPLETED / FAILED                                        |
| stripe_payment_intent_id    | VARCHAR(255)  | Set once, right after PSP creation                                  |
| version                     | BIGINT        | Optimistic locking                                                  |
| created_at                  | TIMESTAMP     |                                                                     |

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
PENDING → COMPLETED
       ↘ FAILED
```

There is intentionally no `PROCESSING` state. Stripe does not guarantee an intermediate "processing" event for standard card payments confirmed without redirect-based methods - the state machine models what the PSP actually reports, rather than a step that would sometimes have to be synthesized to satisfy the model.

Status transitions are validated inside the `Payment` aggregate. Calling `complete()`/`fail()` on a payment that is not `PENDING` throws `InvalidPaymentStatusException` (409 Conflict).

---

## Stripe Integration

### Payment creation flow

1. Client calls `POST /api/v1/payments` with a valid JWT and an `Idempotency-Key` header.
2. `PaymentService` creates the `Payment` aggregate (`PENDING`), then calls Stripe through the `PspClient` port to create a `PaymentIntent`.
3. The returned `stripePaymentIntentId` is attached to the payment, and the payment is persisted in a single `save()` call.
4. The response includes a one-time `clientSecret`, used by the frontend to confirm the payment with Stripe.js. Subsequent `GET` requests never return the `clientSecret` again.

If Stripe is unreachable or rejects the request, no row is written to the database - the PSP call happens before the first `save()`, so a failed PSP call never leaves a partial payment behind. The client receives `502 Bad Gateway`.

### Webhook

`POST /api/v1/webhooks/stripe` - public endpoint (no JWT required), secured instead by Stripe's own signature scheme.

- The endpoint verifies the `Stripe-Signature` header against `stripe.webhook-secret` before processing anything.
- Only `payment_intent.succeeded` and `payment_intent.payment_failed` are handled; every other event type is acknowledged with `200 OK` and ignored, so Stripe never retries events this service was never meant to process.
- If the event's data object can't be deserialized against the SDK's pinned API version, the handler falls back to `deserializeUnsafe()`; if that also fails, the endpoint returns `500` so Stripe retries instead of the payment silently staying `PENDING` forever.
- Duplicate delivery of the same event is idempotent: if the payment is already in the target terminal status, the handler is a no-op. Stripe guarantees at-least-once delivery, so this is expected, not exceptional.

### Ownership

`senderId` is never accepted from the request body. It is resolved from the authenticated principal (`UserPrincipal`, built from the JWT) inside the controller and passed explicitly to `PaymentService.create()`. This closes an IDOR (Insecure Direct Object Reference) hole where a client could otherwise create a payment "as" any arbitrary user by supplying their UUID.

### Manual status override

`PATCH /{id}/complete` and `PATCH /{id}/fail` remain available for manual reconciliation (e.g. a webhook delivery was missed) but are restricted to `ADMIN` via `@PreAuthorize("hasRole('ADMIN')")` - Stripe's webhook is the only path a regular merchant's payment can complete through.

---

## Running Locally

### Prerequisites
- Docker Desktop
- Java 21
- Maven
- [Stripe CLI](https://docs.stripe.com/stripe-cli) (for local webhook forwarding)

### Environment variables

Copy `.env.example` to `.env` and fill in the values:

```bash
cp .env.example .env
```

Required Stripe-related variables:

```
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

### Start infrastructure

```bash
docker-compose up -d
```

This starts PostgreSQL on port `5432` and Redis on port `6379` by default, you can configure it changing the env variables.

### Forward Stripe webhooks locally

```bash
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe
```

Copy the `whsec_...` secret printed by the CLI into `STRIPE_WEBHOOK_SECRET` in `.env`. This secret is session-specific and changes every time `stripe listen` restarts.

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

`senderId` is derived from the JWT - it is not part of the request body.

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Idempotency-Key: unique-key-123" \
  -d '{
    "recipientId": "UUID_2",
    "amount": 100.00,
    "currency": "USD"
  }'
```

Response:
```json
{
  "id": "b0e3...",
  "senderId": "90e3...",
  "recipientId": "UUID_2",
  "amount": 100.0000,
  "currency": "USD",
  "status": "PENDING",
  "stripePaymentIntentId": "pi_3P...",
  "clientSecret": "pi_3P..._secret_...",
  "createdAt": "2026-01-01T12:00:00"
}
```

Use `clientSecret` on the frontend with `stripe.confirmPayment()` (or equivalent) to complete the payment. The service updates the payment's status automatically once Stripe's webhook confirms the outcome - no further client action is required against this API.

### Get a payment

```bash
curl http://localhost:8080/api/v1/payments/{id} \
  -H "Authorization: Bearer <token>"
```

`clientSecret` is `null` here - it is only returned once, at creation time.

### Manually complete a payment (ADMIN only)

```bash
curl -X PATCH http://localhost:8080/api/v1/payments/{id}/complete \
  -H "Authorization: Bearer <admin-token>"
```

### Manually fail a payment (ADMIN only)

```bash
curl -X PATCH http://localhost:8080/api/v1/payments/{id}/fail \
  -H "Authorization: Bearer <admin-token>"
```

---

## Error Responses

All errors follow RFC 7807 Problem Details:

| Status | Title                         | When                                                    |
|--------|-------------------------------|---------------------------------------------------------|
| 400    | Validation failed             | Invalid request body                                    |
| 400    | Missing required header       | `Idempotency-Key` not provided                          |
| 401    | Unauthorized                  | Missing or invalid JWT token / incorrect password       |
| 403    | Forbidden                     | Non-`ADMIN` calling `/complete` or `/fail`              |
| 404    | Payment not found             | Payment ID (or Stripe PaymentIntent ID) does not exist  |
| 409    | Invalid payment status        | Illegal state transition                                |
| 502    | Payment provider unavailable  | Stripe API call failed or was unreachable               |

---

## Key Architectural Decisions

### JWT Authentication
All endpoints except `/api/v1/auth/**`, Swagger UI, and the Stripe webhook require a valid JWT token. Tokens are signed with HMAC-SHA384, contain the username and role in the payload, and expire after 24 hours. The `JwtAuthenticationFilter` validates the token on every request before it reaches the controller. The authenticated principal is exposed as a custom `UserPrincipal` carrying the user's `UUID`, so downstream code never needs a second database lookup to resolve identity.

### Idempotency (API)
Every `POST /payments` request requires an `Idempotency-Key` header. The key and its response are stored in Redis with a 24-hour TTL. Duplicate requests return the cached response without creating a new payment or calling Stripe again. Implemented as a reusable `@Idempotent` AOP annotation.

### Idempotency (Webhook)
Stripe delivers webhook events at-least-once. `PaymentService` treats a webhook event that targets an already-terminal status as a no-op rather than an error, so retried deliveries don't fail loudly or corrupt the audit trail with duplicate transitions.

### Hexagonal Architecture
The `Payment` domain class is a plain Java object with no framework annotations. It contains all business logic including state machine validation. JPA mapping lives in a separate `PaymentJpaEntity` in the infrastructure layer. Stripe is accessed only through the `PspClient` port; `StripeClientImpl` is the sole adapter that knows Stripe exists. Domain logic can be unit-tested without starting Spring, a database, or a network call.

### Optimistic Locking
`PaymentJpaEntity` uses `@Version` to prevent lost updates under concurrent requests. If two requests attempt to update the same payment simultaneously, the second one receives an `OptimisticLockException`.

### Audit Trail
Every status change is recorded in `payment_history` within the same transaction as the payment update, after the payment row itself is persisted (required by the FK from `payment_history` to `payments`). Timestamps for transitions that straddle a network call (e.g. the PSP call during creation) are captured at the moment the domain mutation actually happens, not at the moment the audit row is written - otherwise network latency to Stripe would silently distort the recorded history.

### PSP Communication Failures
Failures talking to Stripe are translated into a dedicated `PspCommunicationException` and mapped to `502 Bad Gateway`, distinguishing "the payment provider failed" from "this service has a bug" (`500`). Because the PSP call happens before the payment is first persisted, a failed call never leaves an orphaned or partial payment row behind.

### AOP Logging
Method entry, exit, and execution time are logged via `LoggingAspect` without polluting business logic. Exception handlers are logged separately with a concise message rather than a full stack trace. Log output is split by profile: application logs go to both console and file, Spring framework logs go to console only.

---

## Running tests

```bash
mvn test
```

Tests use Testcontainers - Docker must be running. PostgreSQL and Redis containers start automatically and are shared across all test classes extending `BaseIntegrationTest`. Integration tests exercise the real security filter chain (JWT authentication via `JwtService`, real users persisted per test) rather than mocking authentication. Stripe calls are mocked in tests (`PspClient` and `PaymentService` are Spring beans easily replaced with `@MockitoBean`); no real network calls to Stripe are made during the test suite.

### Test coverage
- **Unit tests** - `Payment`, `Money` domain logic; `JwtService` token generation and validation; `AuthService`, `PaymentService` with Mockito mocks
- **Integration tests** - repository layer with real PostgreSQL
- **End-to-end tests** - full HTTP stack with `MockMvc`: payment lifecycle, status transitions, idempotency, authentication and registration, Stripe webhook handling (signature verification, event routing, idempotent delivery)

### Coverage reports (JaCoCo)

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.15</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

`report` is bound to the `test` phase, so a plain `mvn clean test` regenerates the report - no separate `jacoco:report` goal needed.

```bash
mvn clean test
open target/site/jacoco/index.html   # HTML report, per-package and per-class breakdown
```