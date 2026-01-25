package com.pinkkila.roomreservationapi.reservation;

import java.time.OffsetDateTime;

public record ReservationResponse(
    Long id,
    Integer roomId,
    OffsetDateTime startTime,
    OffsetDateTime endTime
) {
}
