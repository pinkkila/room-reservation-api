package com.pinkkila.roomreservationapi.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("reservation")
public class Reservation {
    @Id
    private Long id;
    private Integer roomId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}
