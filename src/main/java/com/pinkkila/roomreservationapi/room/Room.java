package com.pinkkila.roomreservationapi.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@AllArgsConstructor
@Builder
@Table("room")
public class Room {
    @Id
    private Integer id;
    private String name;
}
