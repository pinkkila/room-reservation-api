# Requirement 3: Delete Meeting Room Reservation (DELETE)

## 1. Overview

The goal is to implement a REST API endpoint that allows clients to delete an existing meeting room reservation by its
identifier.

## 2. Scope

- Method: `DELETE`
- Path: `/api/reservations/{reservationId}`
- Functionality: Delete a meeting room reservation if it exists.

## 3. Business Rules

- Existence Check:
    - A reservation can only be deleted if it exists.
- No Side Effects:
    - Deleting a reservation must not affect other reservations or rooms.

## 4. Acceptance Criteria

- Success (204 No Content):
    - Returned when the reservation is successfully deleted.
    - Response body must be empty.
- Not Found (404 Not Found):
    - Returned if no reservation exists with the given `reservationId`.
- Bad Request (400 Bad Request):
    - Returned if the `reservationId` path variable is invalid (e.g., non-numeric or malformed).
- Error Consistency:
    - All 4xx errors must return a JSON body in problem details format (RFC 9457).
    - Error responses must follow the project's Global Exception Handler conventions.
- Validation:
    - Path variables must be validated before processing.
- Testing:
    - Endpoint is tested with:
        - Service layer tests.
        - Web layer tests.
        - Data layer tests.
        - Integration tests.

## 6. User Stories & API Contract

> As a User: I want to cancel a reservation so that the room becomes available again.
>
> As a Frontend Developer: I want to send a DELETE request with a reservation ID and receive confirmation that the
> reservation has been removed.
