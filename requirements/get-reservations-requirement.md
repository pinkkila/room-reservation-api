# Requirement 2: List (Page) of Meeting Room Reservations (GET)

## 1. Overview

The goal is to implement a REST API endpoint that allows clients to retrieve meeting room reservations in a paginated
and sortable manner, optionally filtered by room.

## 2. Scope

- Method: `GET`
- Path: `/api/reservations`
- Functionality: Retrieve meeting room reservations with pagination, sorting, and optional filtering.

## 3. Business Rules

- Pagination Required:
    - Results must always be paginated.
    - Default pagination values must be applied if not explicitly provided.
- Maximum Page Size:
    - The maximum allowed page size is 100.
- Sorting Safety:
    - Sorting must only be allowed on explicitly whitelisted fields.
    - Invalid or unknown sort fields must be rejected.
- Filtering by Room:
    - When `roomId` is provided, only reservations for that room are returned.
    - `roomId` must be a valid numeric identifier.

## 4. Acceptance Criteria

- Success (200 OK):
    - Returns a paginated list of reservations.
- Bad Request (400 Bad Request):
    - Invalid `roomId` value (e.g. non-numeric or negative).
    - Page size exceeds the maximum allowed value (100).
    - Invalid or non-whitelisted sort parameters.
    - Invalid page or size parameters.
- (Room) Not Found (404 Not Found):
    - Room with id does not found if `roomId` is provided and does not exist.
- Error Consistency:
    - All 4xx errors must return a JSON body in problem details format (RFC 9457).
    - Error responses must follow the project's Global Exception Handler conventions.
- Validation:
    - Query parameters must be validated and sanitized before use.
    - Query parameter is validated with @Valid using a dedicated query param object.
- Sorting & Pagination Defaults:
    - Default sorting field: `startTime` and order `asc`.
    - Default page size: 20.
- Testing:
    - Endpoint is tested with:
        - Service layer tests.
        - Web layer tests.
        - Data layer tests.
        - Integration tests.

## 5. Sorting Rules

- Sorting is only allowed on predefined fields (e.g. `id`, `startTime`, `endTime`, `roomId`).
- Any attempt to sort by a non-whitelisted field must result in a 400 Bad Request.


## 7. User Stories & API Contract

> As a User: I want to view reservations so that I can see which rooms are booked and when.
>
> As a Frontend Developer: I want to fetch reservations with pagination, sorting, and optional filtering by room.