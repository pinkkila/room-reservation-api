package com.pinkkila.roomreservationapi.reservation;

import com.pinkkila.roomreservationapi.validation.Allowlist;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record ReservationQuery(
        @Min(0)
        Integer page,

        @Min(1)
        @Max(100)
        Integer size,

        @Allowlist(
                value = {"id", "startTime", "endTime", "roomId"},
                message = "Invalid sort field. Allowed fields are: {value}"
        )
        String sortBy,
        
        @Allowlist(
                value = {"asc", "desc"},
                message = "Sort order must be 'asc' or 'desc'"
        )
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
