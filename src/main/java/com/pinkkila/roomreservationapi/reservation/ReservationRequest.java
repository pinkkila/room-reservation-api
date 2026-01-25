package com.pinkkila.roomreservationapi.reservation;

import com.pinkkila.roomreservationapi.reservation.validation.ValidReservationRange;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

@ValidReservationRange
public record ReservationRequest(
    @NotNull(message = "Room ID is required")
    Integer roomId,
    
    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    OffsetDateTime startTime,
    
    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    OffsetDateTime endTime
) {
}
