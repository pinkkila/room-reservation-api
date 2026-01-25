package com.pinkkila.roomreservationapi.reservation;

import com.pinkkila.roomreservationapi.reservation.exception.ReservationNotFoundException;
import com.pinkkila.roomreservationapi.reservation.exception.RoomNotFoundException;
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
import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.web.server.ResponseStatusException;

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

        // 3.2 Validate sortBy against whitelist
        List<String> allowedSortFields = List.of("id", "startTime", "endTime", "roomId");
        if (!allowedSortFields.contains(query.sortBy())) {
            log.warn("Invalid sortBy field: {}", query.sortBy());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sortBy field: " + query.sortBy());
        }

        // 3.3 Verify room existence if roomId provided
        if (query.roomId() != null && !roomRepository.existsById(query.roomId())) {
            log.warn("Room not found: {}", query.roomId());
            throw new RoomNotFoundException("Room with ID " + query.roomId() + " not found");
        }

        // 3.4 Build PageRequest and call repository
        Sort.Direction direction = Sort.Direction.fromString(query.sortOrder());
        Pageable pageable = PageRequest.of(query.page(), query.size(), Sort.by(direction, query.sortBy()));

        Page<Reservation> reservationsPage;
        if (query.roomId() != null) {
            reservationsPage = reservationRepository.findByRoomId(query.roomId(), pageable);
        } else {
            reservationsPage = reservationRepository.findAll(pageable);
        }

        // 3.5 Map Page<Reservation> to Page<ReservationResponse>
        return reservationsPage.map(this::mapToResponse);
    }

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        log.info("Creating reservation for room: {}", request.roomId());

        if (!roomRepository.existsById(request.roomId())) {
            log.warn("Room not found: {}", request.roomId());
            throw new RoomNotFoundException("Room with ID " + request.roomId() + " not found");
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
            log.warn("Reservation not found: {}", reservationId);
            throw new ReservationNotFoundException("Reservation with ID " + reservationId + " not found");
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
