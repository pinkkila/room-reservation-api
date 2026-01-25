package com.pinkkila.roomreservationapi.testdata;

import com.pinkkila.roomreservationapi.reservation.Reservation;
import com.pinkkila.roomreservationapi.reservation.ReservationRequest;
import com.pinkkila.roomreservationapi.reservation.ReservationResponse;
import java.time.OffsetDateTime;

/**
 * Utility for creating Reservation test data.
 * Combines Object Mother (static factory methods) with the Builder pattern.
 */
public class ReservationTestData {

    /**
     * Returns a builder pre-configured for a 1-hour reservation.
     */
    public static ReservationBuilder anOneHourReservation() {
        return new ReservationBuilder();
    }

    public static class ReservationBuilder {
        private Integer roomId;
        private OffsetDateTime startTime = OffsetDateTime.now().plusDays(1).withNano(0);
        private OffsetDateTime endTime; // Optional override

        public ReservationBuilder withRoom(Integer roomId) {
            this.roomId = roomId;
            return this;
        }

        public ReservationBuilder withStartTime(OffsetDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public ReservationBuilder withEndTime(OffsetDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Reservation build() {
            // Encapsulates the logic: if no endTime is specified, it's 1 hour after startTime.
            OffsetDateTime finalEndTime = (endTime != null) ? endTime : startTime.plusHours(1);

            return Reservation.builder()
                    .roomId(roomId)
                    .startTime(startTime)
                    .endTime(finalEndTime)
                    .build();
        }

        public ReservationRequest asRequest() {
            OffsetDateTime finalEndTime = (endTime != null) ? endTime : startTime.plusHours(1);
            return new ReservationRequest(roomId, startTime, finalEndTime);
        }

        public ReservationResponse asResponse(Long id) {
            OffsetDateTime finalEndTime = (endTime != null) ? endTime : startTime.plusHours(1);
            return new ReservationResponse(id, roomId, startTime, finalEndTime);
        }
    }
}
