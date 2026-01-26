package com.pinkkila.roomreservationapi.reservation;

import com.pinkkila.roomreservationapi.reservation.exception.ReservationNotFoundException;
import com.pinkkila.roomreservationapi.room.exception.RoomNotFoundException;
import com.pinkkila.roomreservationapi.room.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservations(ReservationQuery query) {
        log.info("Fetching reservations with query: {}", query);

        List<String> allowedSortFields = List.of("id", "startTime", "endTime", "roomId");
        if (!allowedSortFields.contains(query.sortBy())) {
            log.warn("Invalid sortBy field: {}", query.sortBy());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sortBy field: " + query.sortBy());
        }

        if (query.roomId() != null && !roomRepository.existsById(query.roomId())) {
            throw new RoomNotFoundException(query.roomId());
        }

        Sort.Direction direction = Sort.Direction.fromString(query.sortOrder());
        Pageable pageable = PageRequest.of(query.page(), query.size(), Sort.by(direction, query.sortBy()));

        Page<Reservation> reservationsPage;
        if (query.roomId() != null) {
            reservationsPage = reservationRepository.findByRoomId(query.roomId(), pageable);
        } else {
            reservationsPage = reservationRepository.findAll(pageable);
        }

        return reservationsPage.map(this::mapToResponse);
    }

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        log.info("Creating reservation for room: {}", request.roomId());

        if (!roomRepository.existsById(request.roomId())) {
            throw new RoomNotFoundException(request.roomId());
        }

        Reservation reservation = Reservation.builder()
                .roomId(request.roomId())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("Reservation created with ID: {}", savedReservation.getId());

        return mapToResponse(savedReservation);
    }

    @Transactional
    public void deleteReservation(Long reservationId) {
        log.info("Deleting reservation: {}", reservationId);

        if (!reservationRepository.existsById(reservationId)) {
            throw new ReservationNotFoundException(reservationId);
        }

        reservationRepository.deleteById(reservationId);
        log.info("Reservation deleted successfully: {}", reservationId);
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getRoomId(),
                reservation.getStartTime(),
                reservation.getEndTime()
        );
    }
}
