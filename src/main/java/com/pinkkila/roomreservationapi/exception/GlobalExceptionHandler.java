package com.pinkkila.roomreservationapi.exception;

import com.pinkkila.roomreservationapi.reservation.exception.ReservationNotFoundException;
import com.pinkkila.roomreservationapi.room.exception.RoomNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.lang.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    private ProblemDetail createProblemDetail(HttpStatus status, String detail, ErrorType errorType) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(errorType.getTitle());
        problemDetail.setType(errorType.toUrn());
        return problemDetail;
    }
    
    @ExceptionHandler(RoomNotFoundException.class)
    public ProblemDetail handleRoomNotFoundException(RoomNotFoundException ex) {
        log.warn("Room not found: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), ErrorType.ROOM_NOT_FOUND);
    }
    
    @ExceptionHandler(ReservationNotFoundException.class)
    public ProblemDetail handleReservationNotFoundException(ReservationNotFoundException ex) {
        log.warn("Reservation not found: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), ErrorType.RESERVATION_NOT_FOUND);
    }
    
    /**
     * Handles {@link DataIntegrityViolationException} by mapping PostgreSQL constraint violations
     * to specific {@link ProblemDetail} responses.
     * <p>
     * <b>Defense in Depth Strategy:</b>
     * Most constraints handled here (CHECK constraints and Foreign Keys) are already validated
     * at the application level (Jakarta Bean Validation and manual service checks).
     * If application-level validation is functioning correctly, only the exclusion constraint
     * ('reservation_overlap_excl') should ever be triggered under normal circumstances
     * (e.g., due to race conditions).
     * <p>
     * Triggering any other constraint indicates a bypass or failure in the application's
     * validation logic and is logged as an error.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(ex);
        
        if (cause instanceof org.postgresql.util.PSQLException psqlException) {
            var serverError = psqlException.getServerErrorMessage();
            if (serverError != null && serverError.getConstraint() != null) {
                String constraint = serverError.getConstraint();
                
                return switch (constraint) {
                    case "reservation_overlap_excl" -> {
                        log.warn("Database constraint violation: 'reservation_overlap_excl' - overlapping reservation occurred.");
                        yield createProblemDetail(HttpStatus.CONFLICT, "The room is already reserved for the requested time period.", ErrorType.OVERLAPPING_RESERVATION);
                    }
                    case "start_before_end" -> {
                        log.error("Validation bypass detected: 'start_before_end' constraint violated. Check @ValidReservationRange logic.", ex);
                        yield createProblemDetail(HttpStatus.BAD_REQUEST, "The start time must be before the end time.", ErrorType.INVALID_RESERVATION_TIME);
                    }
                    case "start_in_future" -> {
                        log.error("Validation bypass detected: 'start_in_future' constraint violated. Check @Future annotations in ReservationRequest.", ex);
                        yield createProblemDetail(HttpStatus.BAD_REQUEST, "The reservation must start in the future.", ErrorType.INVALID_RESERVATION_TIME);
                    }
                    case "reservation_room_id_fkey" -> {
                        log.error("Validation bypass detected: 'reservation_room_id_fkey' constraint violated. Check existence check in ReservationService.", ex);
                        yield createProblemDetail(HttpStatus.NOT_FOUND, "The requested room does not exist.", ErrorType.ROOM_NOT_FOUND);
                    }
                    default -> {
                        log.error("Unexpected database constraint violation: {}", constraint, ex);
                        yield createProblemDetail(HttpStatus.BAD_REQUEST, "Something went wrong when creating reservation.", ErrorType.RESERVATION_ERROR);
                    }
                };
            }
        }
        
        log.error("Unexpected data integrity violation", ex);
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Something went wrong when creating reservation.", ErrorType.RESERVATION_ERROR);
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("Method argument type mismatch: {}", ex.getMessage());
        return createProblemDetail(HttpStatus.BAD_REQUEST, "One or more path parameters are invalid.", ErrorType.INVALID_PATH_PARAMETER);
    }
    
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        
        boolean isRequestBody = ex.getParameter().hasParameterAnnotation(RequestBody.class);
        ErrorType errorType = isRequestBody ? ErrorType.INVALID_REQUEST_BODY : ErrorType.INVALID_REQUEST_PARAMETERS;
        String detail = isRequestBody
                ? "The data provided in the request body is invalid."
                : "One or more request parameters provided in the URL are invalid.";
        
        ProblemDetail problemDetail = createProblemDetail(HttpStatus.valueOf(status.value()), detail, errorType);
        
        Map<String, String> errors = ex.getBindingResult().getAllErrors().stream()
                .collect(Collectors.toMap(
                        error -> error instanceof FieldError fieldError ? fieldError.getField() : "Unknown field",
                        this::resolveErrorMessage,
                        (existing, replacement) -> existing + ", " + replacement
                ));
        
        problemDetail.setProperty("errors", errors);
        
        log.warn("Validation failed for {}: fields={}", isRequestBody ? "body" : "parameters", errors.keySet());
        
        return createResponseEntity(problemDetail, headers, status, request);
    }
    
    /**
     * If the error is a binding failure (like a type mismatch) -> return "Invalid value".
     */
    private String resolveErrorMessage(ObjectError error) {
        if (error instanceof FieldError fieldError && fieldError.isBindingFailure()) {
            return "Invalid value";
        }
        String message = error.getDefaultMessage();
        return (message != null) ? message : "Invalid value";
    }
    
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        log.warn("Failed to read request: Malformed JSON or invalid format");
        
        ProblemDetail problemDetail = createProblemDetail(HttpStatus.valueOf(status.value()), "Malformed or invalid JSON payload", ErrorType.INVALID_REQUEST_BODY);
        return createResponseEntity(problemDetail, headers, status, request);
    }
    
    
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        
        Set<String> fields = ex.getParameterValidationResults().stream()
                .map(result -> result.getMethodParameter().getParameterName() != null
                        ? result.getMethodParameter().getParameterName()
                        : "parameter")
                .collect(Collectors.toSet());
        
        log.warn("Method validation failed: fields={}", fields);

        ProblemDetail problemDetail = createProblemDetail(HttpStatus.valueOf(status.value()), "One or more path parameters are invalid.", ErrorType.INVALID_PATH_PARAMETER);
        return createResponseEntity(problemDetail, headers, status, request);
    }
    
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex) {
        log.error("An unexpected error occurred", ex);
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred.", ErrorType.INTERNAL_SERVER_ERROR);
    }
    
    
}
