package com.pinkkila.roomreservationapi.reservation;

import com.pinkkila.roomreservationapi.reservation.exception.ReservationNotFoundException;
import com.pinkkila.roomreservationapi.room.exception.RoomNotFoundException;
import com.pinkkila.roomreservationapi.room.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTests {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Captor
    private ArgumentCaptor<Reservation> reservationCaptor;

    @Nested
    @DisplayName("Creating Reservations")
    class CreatingReservations {

        @Test
        @DisplayName("Should create reservation successfully when room exists")
        void shouldCreateReservationSuccessfully() {
            // Given
            int roomId = 1;
            OffsetDateTime start = OffsetDateTime.now().plusDays(1);
            OffsetDateTime end = start.plusHours(1);
            ReservationRequest request = new ReservationRequest(roomId, start, end);

            when(roomRepository.existsById(roomId)).thenReturn(true);
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
                Reservation res = invocation.getArgument(0);
                return Reservation.builder()
                        .id(100L)
                        .roomId(res.getRoomId())
                        .startTime(res.getStartTime())
                        .endTime(res.getEndTime())
                        .build();
            });

            // When
            ReservationResponse response = reservationService.createReservation(request);

            // Then
            verify(reservationRepository).save(reservationCaptor.capture());
            Reservation savedReservation = reservationCaptor.getValue();

            assertThat(savedReservation.getRoomId()).isEqualTo(roomId);
            assertThat(savedReservation.getStartTime()).isEqualTo(start);
            assertThat(savedReservation.getEndTime()).isEqualTo(end);

            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.roomId()).isEqualTo(roomId);
            assertThat(response.startTime()).isEqualTo(start);
            assertThat(response.endTime()).isEqualTo(end);

            verify(roomRepository).existsById(roomId);
        }

        @Test
        @DisplayName("Should throw RoomNotFoundException when room does not exist")
        void shouldThrowRoomNotFoundException() {
            // Given
            ReservationRequest request = new ReservationRequest(
                    99,
                    OffsetDateTime.now().plusDays(1),
                    OffsetDateTime.now().plusDays(1).plusHours(1)
            );

            when(roomRepository.existsById(99)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(RoomNotFoundException.class)
                    .hasMessageContaining("Room with ID 99 not found");

            verify(roomRepository).existsById(99);
            verifyNoInteractions(reservationRepository);
        }
    }

    @Nested
    @DisplayName("Getting Reservations")
    class GettingReservations {

        @Test
        @DisplayName("Should fetch all reservations when no roomId filter is provided")
        void shouldFetchAllReservations() {
            // Given
            ReservationQuery query = new ReservationQuery(0, 20, "startTime", "asc", null);
            Reservation reservation = Reservation.builder()
                    .id(1L)
                    .roomId(10)
                    .startTime(OffsetDateTime.now())
                    .endTime(OffsetDateTime.now().plusHours(1))
                    .build();

            when(reservationRepository.findAll(any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(reservation)));

            // When
            Page<ReservationResponse> result = reservationService.getReservations(query);

            // Then
            assertThat(result.getContent()).hasSize(1);
            ReservationResponse response = result.getContent().get(0);
            assertThat(response.id()).isEqualTo(reservation.getId());
            assertThat(response.roomId()).isEqualTo(reservation.getRoomId());

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(reservationRepository).findAll(pageableCaptor.capture());
            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(0);
            assertThat(pageable.getPageSize()).isEqualTo(20);
            assertThat(pageable.getSort().getOrderFor("startTime").getDirection()).isEqualTo(org.springframework.data.domain.Sort.Direction.ASC);

            verifyNoInteractions(roomRepository);
        }

        @Test
        @DisplayName("Should fetch reservations for specific room when roomId filter is provided")
        void shouldFetchReservationsByRoomId() {
            // Given
            int roomId = 10;
            ReservationQuery query = new ReservationQuery(1, 10, "endTime", "desc", roomId);
            Reservation reservation = Reservation.builder()
                    .id(1L)
                    .roomId(roomId)
                    .startTime(OffsetDateTime.now())
                    .endTime(OffsetDateTime.now().plusHours(1))
                    .build();

            when(roomRepository.existsById(roomId)).thenReturn(true);
            when(reservationRepository.findByRoomId(eq(roomId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(reservation)));

            // When
            Page<ReservationResponse> result = reservationService.getReservations(query);

            // Then
            assertThat(result.getContent()).hasSize(1);
            ReservationResponse response = result.getContent().get(0);
            assertThat(response.roomId()).isEqualTo(roomId);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(reservationRepository).findByRoomId(eq(roomId), pageableCaptor.capture());
            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(1);
            assertThat(pageable.getPageSize()).isEqualTo(10);
            assertThat(pageable.getSort().getOrderFor("endTime").getDirection()).isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
        }

        @Test
        @DisplayName("Should throw RoomNotFoundException when filtering by non-existent roomId")
        void shouldThrow404WhenRoomNotFound() {
            // Given
            ReservationQuery query = new ReservationQuery(0, 20, "startTime", "asc", 999);
            when(roomRepository.existsById(999)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> reservationService.getReservations(query))
                    .isInstanceOf(RoomNotFoundException.class)
                    .hasMessageContaining("Room with ID 999 not found");
        }
    }

    @Nested
    @DisplayName("Deleting Reservations")
    class DeletingReservations {
        @Test
        @DisplayName("Should delete reservation successfully when it exists")
        void shouldDeleteReservationSuccessfully() {
            // Given
            Long reservationId = 1L;
            when(reservationRepository.existsById(reservationId)).thenReturn(true);

            // When
            reservationService.deleteReservation(reservationId);

            // Then
            verify(reservationRepository).existsById(reservationId);
            verify(reservationRepository).deleteById(reservationId);
        }

        @Test
        @DisplayName("Should throw ReservationNotFoundException when reservation does not exist")
        void shouldThrowReservationNotFoundException() {
            // Given
            Long reservationId = 99L;
            when(reservationRepository.existsById(reservationId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> reservationService.deleteReservation(reservationId))
                    .isInstanceOf(ReservationNotFoundException.class)
                    .hasMessageContaining("Reservation with ID 99 not found");

            verify(reservationRepository).existsById(reservationId);
            verify(reservationRepository, never()).deleteById(anyLong());
        }
    }
}
