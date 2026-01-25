package com.pinkkila.roomreservationapi.reservation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends ListCrudRepository<Reservation, Long>, PagingAndSortingRepository<Reservation, Long> {

    Page<Reservation> findByRoomId(Integer roomId, Pageable pageable);
}
