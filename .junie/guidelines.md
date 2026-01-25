# Junie Guidelines

These guidelines define the technical and architectural conventions for this project. They serve as the primary source
of truth. Follow them strictly when generating, modifying, or refactoring code.

## Implementation and Code Quality

- Implementation needs to follow best practices in the highest standard possible.

- Implementation needs to be:
  - **Maintainable**
  - **Scalable**
  - **Professional**
  - **Production-ready**
  - **Enterprise-level**
  - **Testable**

- Before implementing a new helper method, utility, or logic, search the codebase to see if a solution already exists.
- If a specific problem (like Pagination or Filtering) has already been solved in one feature, follow the same pattern
  in new features to maintain a consistent developer experience.

## Project Structure

- Use a **Package-by-Feature** structure.

Example structure:

```
com.pinkkila.roomreservationapi
├── exception
│   └── GlobalExceptionHandler.java
├── reservation
│   ├── exception
│   │   └── ReservationNotFoundException.java
│   ├── Reservation.java
│   ├── ReservationController.java
│   ├── ReservationRepository.java
│   └── ReservationService.java
│
├── room
│   ├── Room.java
│   └── RoomRepository.java
│ 
└── RoomReservationApiApplication.java
```

- Project uses application.yaml (no applicatoin.properties or application.yml).

## Variable names

- Use clear and descriptive variable names.
- Derive variable names from the class type when convenient.
- Do not use one-character variable names (except for extremely short-lived loop counters like `i`).
- Two or three-character abbreviations are only allowed if they are industry standard:
  - `Exception ex`
  - `Logger log`
  - `UriComponentsBuilder ucb`

## API and Business Logic Architecture

- **Services:**
  - Implement business logic and orchestration in Service classes.
- **Controllers:**
  - Zero business logic. Controllers only handle HTTP mapping, routing, and response status codes.
  - Always return `ResponseEntity<T>` to be explicit about the HTTP status.
  - Use DTOs (prefer `records`) for all requests and responses. Never expose Entities.
  - Use separate `records` for query parameters (e.g., `ReservationQuery`).
  - Use `@Valid` (from `jakarta.validation`) to validate request bodies and query parameters at the entry point.
  - Use pagination for collection resources that may contain an unbounded number of items.

## Database and Data Persistence

- Project uses a real **PostgreSQL** database via **Docker**.
- Database schema and data are initialized using Spring Boot's built-in support (`spring.sql.init.mode: always`).
- **Flyway or Liquibase are not used.** This is the intentional exception to the "Production-ready" and "Enterprise-level" requirement for this project.

## Exception handling

- Use a single `@RestControllerAdvice` class to centralize error handling.
- Return consistent error responses. Use ProblemDetails response format (RFC 9457).
- Strictly never expose to the client:
  - stack traces
  - SQL errors
  - internal implementation details
  - any confidential data

## Logging

- Use SLF4J via Lombok's `@Slf4j` for logging.
- Never use System.out.println() for application logging.
- Ensure that **PII (Personally Identifiable Information)**, credentials, tokens, or other confidential data are never
  written to the logs.
- Use placeholders (`{}`) instead of string concatenation to readability:
  `log.info("Processing reservation: {}", reservationId);`
- Log Levels:
  - `ERROR`: For system failures and unexpected exceptions. Always log the exception object:
    `log.error("Critical error occurred", ex);`
  - `WARN`: For non-critical issues or unexpected behavior that doesn't break the system.
  - `INFO`: For significant lifecycle events or key business transactions.
  - `DEBUG`: For detailed flow analysis during development.

## Testing

- When convenient, oragnize test using `@Nested`.
- Use `@DisplayName` for easier readability.
- For Service classes, use:
  - `@ExtendWith(MockitoExtension.class)`
  - `@InjectMocks`
- For Controller classes, use:
  - `@WebMvcTest`
  - `@MockitoBean`
- For Repository classes, use:
  - `@DataJdbcTest`
  - `@Import(TestcontainersConfiguration.class)`
  - `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`
  - `@ActiveProfiles("test")`
- For Full Integration tests, use:
  - `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`
  - `@Import(TestcontainersConfiguration.class)`
  - `@ActiveProfiles("test")`
  - `WebTestClient`
