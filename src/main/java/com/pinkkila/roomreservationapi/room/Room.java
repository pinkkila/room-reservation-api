package com.pinkkila.roomreservationapi.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity representing a room available for reservation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("room")
public class Room {
    @Id
    private Integer id;
    private String name;
}
