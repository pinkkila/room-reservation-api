package com.pinkkila.roomreservationapi.room;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Room entity.
 */
@Repository
public interface RoomRepository extends ListCrudRepository<Room, Integer> {
}
