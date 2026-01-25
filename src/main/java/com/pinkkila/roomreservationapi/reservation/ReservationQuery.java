package com.pinkkila.roomreservationapi.reservation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * DTO for reservation query parameters.
 * Defaults are applied in the compact constructor.
 */
public record ReservationQuery(
        @Min(0)
        Integer page,

        @Min(1)
        @Max(100)
        Integer size,

        String sortBy,

        @Pattern(regexp = "^(asc|desc)$")
        String sortOrder,

        @Positive
        Integer roomId
) {
    public ReservationQuery {
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
        if (sortBy == null) {
            sortBy = "startTime";
        }
        if (sortOrder == null) {
            sortOrder = "asc";
        }
    }
}
