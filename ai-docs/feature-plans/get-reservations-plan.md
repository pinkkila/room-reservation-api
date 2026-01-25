# Plan: Requirement 2 - Get Reservations

## 1. Repository Layer
- [x] 1.1 Update `ReservationRepository` to extend `PagingAndSortingRepository<Reservation, Long>`.
- [x] 1.2 Add `Page<Reservation> findByRoomId(Integer roomId, Pageable pageable)` to `ReservationRepository`.

## 2. DTOs
- [x] 2.1 Create `ReservationQuery` record with fields: `page`, `size`, `sortBy`, `sortOrder`, `roomId`.
- [x] 2.2 Add a compact constructor to `ReservationQuery` to apply defaults:
    - `page` = 0
    - `size` = 20
    - `sortBy` = "startTime"
    - `sortOrder` = "asc"
- [x] 2.3 Apply Jakarta Validation on the record fields (ensuring they work with the defaulted values):
    - `@Min(0)` for `page`.
    - `@Min(1)`, `@Max(100)` for `size`.
    - `@Positive` for `roomId`.
    - `@Pattern(regexp = "^(asc|desc)$")` for `sortOrder` (case-sensitive).

## 3. Service Layer
- [x] 3.1 Implement `ReservationService.getReservations(ReservationQuery query)`.
- [x] 3.2 Validate `sortBy` against whitelist (`id`, `startTime`, `endTime`, `roomId`) using case-sensitive comparison. Throw 400 if invalid.
- [x] 3.3 If `roomId` provided: verify room existence via `RoomRepository`. Throw `RoomNotFoundException` (404) if missing.
- [x] 3.4 Build `PageRequest` and call repository.
- [x] 3.5 Map `Page<Reservation>` to `Page<ReservationResponse>`. Default Spring Data `Page` JSON structure is acceptable.

## 4. API Layer
- [x] 4.1 Add `GET /api/reservations` to `ReservationController` using `@Valid ReservationQuery`.
- [x] 4.2 Return `200 OK` with paginated body.

## 5. Testing
- [x] 5.1 `ReservationRepositoryTests`: Test pagination and `roomId` filter.
- [x] 5.2 `ReservationServiceTests`: Test sorting whitelist (case-sensitive), `roomId` validation, and mapping.
- [x] 5.3 `ReservationControllerTests`: Test query validation (including case-sensitive `sortOrder` and `sortBy`) and default values.
- [x] 5.4 `GetReservationsIntegrationTests`: E2E tests for success/error scenarios.
