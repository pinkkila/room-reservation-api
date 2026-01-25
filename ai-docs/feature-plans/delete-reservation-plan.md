# Plan: Requirement 3 - Delete Meeting Room Reservation

## 1. Foundation & Exception Handling
- [x] 1.1 Create `ReservationNotFoundException` in `reservation.exception` (mirror `RoomNotFoundException`).
- [x] 1.2 Add `@ExceptionHandler` for `ReservationNotFoundException` in `GlobalExceptionHandler` returning `ProblemDetail` (404 Not Found, title "Reservation Not Found").
- [x] 1.3 Add `@ExceptionHandler` for `MethodArgumentTypeMismatchException` in `GlobalExceptionHandler` returning `ProblemDetail` (400 Bad Request, title "Invalid Parameter Type").

## 2. Service Layer
- [x] 2.1 Implement `ReservationService.deleteReservation(Long reservationId)`.
- [x] 2.2 Verify reservation existence via `ReservationRepository.existsById()`. Throw `ReservationNotFoundException` if missing.
- [x] 2.3 Call `ReservationRepository.deleteById()`.
- [x] 2.4 Log deletion with `reservationId` at `INFO` level.

## 3. API Layer
- [x] 3.1 Add `DELETE /api/reservations/{reservationId}` to `ReservationController` (validate ID with `@Positive`).
- [x] 3.2 Call `ReservationService.deleteReservation()`.
- [x] 3.3 Return `204 No Content`.

## 4. Testing
- [x] 4.1 `ReservationRepositoryTests`: Add test for `deleteById`.
- [x] 4.2 `ReservationServiceTests`: Add unit tests for successful deletion and `ReservationNotFoundException`.
- [x] 4.3 `ReservationControllerTests`: Add unit tests for success (204), missing reservation (404), and invalid ID format (400).
- [x] 4.4 `DeleteReservationIntegrationTests`: Add E2E tests for success and 404 scenarios.

