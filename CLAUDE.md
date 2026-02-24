# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Spring Boot 3.5 REST API for room reservations backed by PostgreSQL. Uses Spring Data JDBC (not JPA). Requires Java 21 and Docker.

## Commands

### Run the application

```bash
./gradlew bootRun
```

Spring Boot Docker Compose automatically starts a PostgreSQL container. App runs at `http://localhost:8080`.

### Run with Docker (full stack)

```bash
docker-compose -f compose.demo.yaml up --build
```

### Run all tests

```bash
./gradlew test
```

Tests use Testcontainers (PostgreSQL), so Docker must be running.

### Run a single test class

```bash
./gradlew test --tests "com.pinkkila.roomreservationapi.reservation.ReservationControllerTests"
```

### Build

```bash
./gradlew build
```

## Architecture

### Package Structure

The codebase is organized by feature, not by layer:

- `reservation/` — the main feature package: `Reservation` (entity), `ReservationController`, `ReservationService`, `ReservationRepository`, `ReservationRequest` (POST body), `ReservationResponse` (DTO), `ReservationQuery` (GET params record with defaults), `reservation/exception/`, `reservation/validation/`
- `room/` — `Room` entity, `RoomRepository`, `room/exception/`
- `exception/` — `GlobalExceptionHandler`, `ErrorType` enum (URN-based), `DatabaseConstraint` enum (maps PostgreSQL constraint names to enum values)
- `validation/` — shared `@Allowlist` annotation + `AllowlistValidator`

### Key Design Decisions

**Spring Data JDBC, not JPA.** Entities use `@Table`, `@Id` from `org.springframework.data`. No lazy loading, no session management. Repositories extend `ListCrudRepository` and `PagingAndSortingRepository`.

**Overlap detection via PostgreSQL exclusion constraint**, not application code. The `reservation_overlap_excl` constraint uses `btree_gist` + `tstzrange`. When a conflict occurs, Spring Data JDBC wraps the `PSQLException` in `DbActionExecutionException`. `GlobalExceptionHandler` unwraps it, reads the constraint name, and maps it to the correct HTTP response.

**RFC 9457 Problem Details** for all error responses. `GlobalExceptionHandler` extends `ResponseEntityExceptionHandler` and returns `ProblemDetail` objects. Error types are identified by URN: `urn:room-reservation-api:<slug>`.

**Dual-layer validation:** `@ValidReservationRange` (class-level annotation) checks that `startTime < endTime`. Field-level `@Future` checks future times. The DB `start_before_end` CHECK constraint is a backstop — if it fires, the handler logs it as a validation bypass error.

**`ReservationQuery` is a record with canonical constructor defaults.** Null values for `page`, `size`, `sortBy`, `sortOrder` are replaced with defaults (0, 20, `startTime`, `asc`) in the compact constructor.

**`sortBy` is allowlisted** via `@Allowlist` to prevent SQL injection through the `Sort.by()` call. Allowed values: `id`, `startTime`, `endTime`, `roomId`.

### Database Schema

Two tables: `room (id SERIAL, name TEXT)` and `reservation (id BIGSERIAL, room_id INT FK, start_time TIMESTAMPTZ, end_time TIMESTAMPTZ)`. Schema runs via `schema.sql` on startup (`spring.sql.init.mode=always`). Demo data from `data.sql` is skipped in tests (`application-test.yaml` sets `data-locations: []`).

### Test Structure

- `ReservationControllerTests` — `@WebMvcTest` with mocked service, tests HTTP layer and `GlobalExceptionHandler`
- `ReservationServiceTests` — unit tests for service logic
- `ReservationRepositoryTests` — tests repository with Testcontainers
- `ReservationFullIntegrationTests` — full stack with Testcontainers
- `TestcontainersConfiguration` — shared `@TestConfiguration` that provides a `PostgreSQLContainer` bean via `@ServiceConnection`
- `ReservationTestData` — Object Mother + Builder pattern for test fixtures

### HTTP test files

The `http/` directory contains `.http` files for manual API testing.
