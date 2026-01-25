# Requirement 1: Create Room Reservation (POST)

## 1. Overview

The goal is to implement a REST API endpoint that allows users to book a meeting room for a specific time slot.

## 2. Scope

- Method: `POST`
- Path: `/api/reservations`
- Functionality: Validate and persist a new meeting room reservation.

## 3. Business Rules

- A room cannot be booked by two different people for the same (or overlapping) time period.
- Reservations must be made for future dates/times only.
- The reservation start time must strictly occur before the end time.

## 4. Acceptance Criteria

- Success (201 Created): Returns the created reservation object in the JSON response body.
- Conflict (409 Conflict): Returned if the room is already booked for the requested time.
    - Prevention of overlapping reservation is hanled using PostgreSQL Exclusion Constraints
- Bad Request (400 Bad Request):
    - Reservation is in the past.
    - End time is before or equal to the start time.
    - The JSON payload is malformed or missing required fields.
- Not Found (404 Not Found): Returned if the specified `roomId` does not exist.
- Error Consistency: All 4xx errors must return a JSON body in problem details (RFC 9457) format containing
  descriptive error information (following the project's Global Exception Handler).
- Validation: Use `@Valid` for request body validation.
- Testing: Endpoint is tested with:
    - Service layer tests.
    - Web layer tests.
    - Data layer tests.
    - Integration tests.

## 5. User Stories & API Contract

> As a User: I want to reserve a meeting room so that I can ensure a space is available for my meeting.
>
> As a Frontend Developer: I want to send a POST request with the following JSON structure to create a reservation
> and receive the saved record in response.

### Request Example

```json
{
  "roomId": 1,
  "startTime": "2026-06-01T12:00:00+02:00",
  "endTime": "2026-06-01T13:00:00+02:00"
}
```

### Response Example

```json
{
  "id": 100,
  "roomId": 1,
  "startTime": "2026-06-01T12:00:00Z",
  "endTime": "2026-06-01T13:00:00Z"
}
```