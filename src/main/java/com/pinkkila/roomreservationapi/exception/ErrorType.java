package com.pinkkila.roomreservationapi.exception;

import lombok.Getter;

import java.net.URI;

public enum ErrorType {
    ROOM_NOT_FOUND("room-not-found", "Room Not Found"),
    RESERVATION_NOT_FOUND("reservation-not-found", "Reservation Not Found"),
    OVERLAPPING_RESERVATION("overlapping-reservation", "Overlapping Reservation"),
    RESERVATION_IN_PAST("reservation-in-past", "Reservation in Past"),
    INVALID_RESERVATION_TIME("invalid-reservation-time", "Invalid Reservation Time"),
    RESERVATION_ERROR("reservation-error", "Reservation Error"),
    INVALID_REQUEST_BODY("invalid-request-body", "Invalid Request Body"),
    INVALID_REQUEST_PARAMETERS("invalid-request-parameters", "Invalid Request Parameters"),
    INVALID_PATH_PARAMETER("invalid-path-parameter", "Invalid Path Parameter"),
    INTERNAL_SERVER_ERROR("internal-server-error", "Internal Server Error");
    
    private static final String BASE_URN = "urn:room-reservation-api:";
    private final String slug;
    @Getter
    private final String title;
    
    ErrorType(String slug, String title) {
        this.slug = slug;
        this.title = title;
    }
    
    public URI toUrn() {
        return URI.create(BASE_URN + slug);
    }
    
}