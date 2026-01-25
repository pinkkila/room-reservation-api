# Plan: Requirement 1 - Create Reservation

## 1. Foundational
- [x] 1.1 `application.yaml`: set `spring.sql.init.mode: always`.
- [x] 1.2 `src/main/resources/schema.sql`:
    - `CREATE EXTENSION IF NOT EXISTS btree_gist;`.
    - `room` table: `id SERIAL PRIMARY KEY`, `name TEXT NOT NULL`.
    - `reservation` table:
        - `id BIGSERIAL PRIMARY KEY`.
        - `room_id INT NOT NULL REFERENCES room(id)`.
        - `start_time TIMESTAMPTZ NOT NULL`.
        - `end_time TIMESTAMPTZ NOT NULL`.
        - `CONSTRAINT start_before_end CHECK (start_time < end_time)`.
        - `CONSTRAINT start_in_future CHECK (start_time >= now())`.
    - `EXCLUDE` constraint: `room_id` equality AND `tstzrange(start_time, end_time)` overlap.
- [x] 1.3 `src/main/resources/data.sql`: Insert three initial rooms.
- [x] 1.4 `exception.GlobalExceptionHandler`:
    - Handle `MethodArgumentNotValidException` -> 400.
    - Handle `HttpMessageNotReadableException` -> 400 (custom errors).
    - Handle `DataIntegrityViolationException` -> 409 (conflict).
    - Handle `RoomNotFoundException` -> 404.
    - Use RFC 9457 ProblemDetails.

## 2. Room Feature
- [x] 2.1 `room.Room` entity.
- [x] 2.2 `room.RoomRepository`.

## 3. Reservation Feature
- [x] 3.1 `reservation.Reservation` entity.
- [x] 3.2 `reservation.ReservationRequest` record:
    - `@NotNull roomId`, `@Future startTime`, `@Future endTime`.
    - Custom validator: `startTime < endTime`.
- [x] 3.3 `reservation.ReservationResponse` record.
- [x] 3.4 `reservation.exception.RoomNotFoundException`.
- [x] 3.5 `reservation.ReservationRepository`.
- [x] 3.6 `reservation.ReservationService`:
    - `@Slf4j` logs.
    - Verify room existence via `RoomRepository`.
    - Map DTO to entity & save.
- [x] 3.7 `reservation.ReservationController`:
    - `POST /api/reservations`.
    - `@Valid`, return `201 Created` + body.

## 4. Testing
- [x] 4.1 `ReservationRepositoryTests`: `@DataJdbcTest`, test exclusion constraint violation.
- [x] 4.2 `ReservationServiceTests`: `@ExtendWith(MockitoExtension.class)`, test logic/validation.
- [x] 4.3 `ReservationControllerTests`: `@WebMvcTest`, test API contract & validation errors.
- [x] 4.4 `CreateReservationIntegrationTests`: `@SpringBootTest`, E2E with Testcontainers.

