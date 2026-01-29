package com.pinkkila.roomreservationapi.reservation;

import com.pinkkila.roomreservationapi.TestcontainersConfiguration;
import com.pinkkila.roomreservationapi.exception.DatabaseConstraint;
import com.pinkkila.roomreservationapi.room.Room;
import com.pinkkila.roomreservationapi.room.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;

import static com.pinkkila.roomreservationapi.testdata.ReservationTestData.anOneHourReservation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJdbcTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ReservationRepositoryTests {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomRepository roomRepository;

    private Integer roomId1;
    private Integer roomId2;

    @BeforeEach
    void setUp() {
        roomId1 = roomRepository.save(Room.builder().name("Room 1").build()).getId();
        roomId2 = roomRepository.save(Room.builder().name("Room 2").build()).getId();
    }
    
    @Nested
    @DisplayName("Save Reservation")
    class SaveReservation {
        
        @Test
        @DisplayName("Should save a valid reservation")
        void shouldSaveValidReservation() {
            OffsetDateTime start = OffsetDateTime.now().plusDays(2).withNano(0);
            OffsetDateTime end = start.plusHours(1);
            Reservation reservation = anOneHourReservation().withRoom(roomId1).withStartTime(start).withEndTime(end).build();
            
            Reservation saved = reservationRepository.save(reservation);
            
            assertThat(saved.getId()).isNotNull();
            
            Reservation reservationFromRepository = reservationRepository.findById(saved.getId()).orElseThrow();
            
            assertThat(reservationFromRepository.getRoomId()).isEqualTo(reservation.getRoomId());
            assertThat(reservationFromRepository.getStartTime()).isEqualTo(reservation.getStartTime());
            assertThat(reservationFromRepository.getEndTime()).isEqualTo(reservation.getEndTime());
            
        }
        
        @Test
        @DisplayName("Should throw exception when reservations overlap")
        void shouldThrowExceptionWhenOverlapping() {
            OffsetDateTime start = OffsetDateTime.now().plusDays(2).withNano(0);
            OffsetDateTime end = start.plusHours(1);
            
            Reservation res1 = anOneHourReservation()
                    .withRoom(roomId1)
                    .withStartTime(start)
                    .withEndTime(end)
                    .build();
            reservationRepository.save(res1);
            
            Reservation res2 = anOneHourReservation()
                    .withRoom(roomId1)
                    .withStartTime(start.plusMinutes(30))
                    .withEndTime(end.plusMinutes(30))
                    .build();
            
            assertThatThrownBy(() -> reservationRepository.save(res2))
                    .isInstanceOf(DbActionExecutionException.class)
                    .hasCauseInstanceOf(DataIntegrityViolationException.class)
                    .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class)
                    .rootCause()
                    .hasMessageContaining(DatabaseConstraint.RESERVATION_OVERLAP.getConstraintName());
        }
        
        @Test
        @DisplayName("Should throw exception when start time is not before end time")
        void shouldThrowExceptionWhenStarTimeIsNotBeforeEndTime() {
            OffsetDateTime start = OffsetDateTime.now().plusDays(2).withNano(0);
            OffsetDateTime end = start.minusHours(1);
            
            Reservation res1 = anOneHourReservation()
                    .withRoom(roomId1)
                    .withStartTime(start)
                    .withEndTime(end)
                    .build();
            
            assertThatThrownBy(() -> reservationRepository.save(res1))
                    .isInstanceOf(DbActionExecutionException.class)
                    .hasCauseInstanceOf(DataIntegrityViolationException.class)
                    .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class)
                    .rootCause()
                    .hasMessageContaining(DatabaseConstraint.START_BEFORE_END.getConstraintName());
        }
        
        @Test
        @DisplayName("Should throw exception when start time is not before end time")
        void shouldThrowExceptionWhenNoRoomIdFound() {
            OffsetDateTime start = OffsetDateTime.now().plusDays(2).withNano(0);
            OffsetDateTime end = start.plusHours(1);
            
            Reservation res1 = anOneHourReservation()
                    .withRoom(999)
                    .withStartTime(start)
                    .withEndTime(end)
                    .build();
            
            assertThatThrownBy(() -> reservationRepository.save(res1))
                    .isInstanceOf(DbActionExecutionException.class)
                    .hasCauseInstanceOf(DataIntegrityViolationException.class)
                    .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class)
                    .rootCause()
                    .hasMessageContaining(DatabaseConstraint.ROOM_ID_FOREIGN_KEY.getConstraintName());
        }
        
    }
    
    @Nested
    @DisplayName("Finding Reservations with Pagination and Filtering")
    class FindingReservations {
        
        @Test
        @DisplayName("Should find all reservations with pagination and sorted with startTime ascending")
        void shouldFindAllWithPagination() {
            // Given
            OffsetDateTime start = OffsetDateTime.now().plusDays(1).withNano(0);
            reservationRepository.save(anOneHourReservation().withRoom(roomId1).withStartTime(start).build());
            reservationRepository.save(anOneHourReservation().withRoom(roomId1).withStartTime(start.plusHours(6)).build());
            reservationRepository.save(anOneHourReservation().withRoom(roomId1).withStartTime(start.plusHours(4)).build());
            reservationRepository.save(anOneHourReservation().withRoom(roomId1).withStartTime(start.plusHours(2)).build());
            reservationRepository.save(anOneHourReservation().withRoom(roomId1).withStartTime(start.plusHours(8)).build());
            
            // When
            Page<Reservation> page = reservationRepository.findAll(PageRequest.of(0, 3, Sort.by("startTime").ascending()));
            
            // Then
            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(2);
            assertThat(page.getNumber()).isZero();
            
            assertThat(page.getContent())
                    .extracting(Reservation::getStartTime)
                    .isSorted();
        }
        
        @Test
        @DisplayName("Should find reservations by roomId with pagination")
        void shouldFindByRoomIdWithPagination() {
            // Given
            OffsetDateTime start = OffsetDateTime.now().plusDays(1).withNano(0);
            reservationRepository.save(anOneHourReservation().withRoom(roomId1).withStartTime(start).build());
            reservationRepository.save(anOneHourReservation().withRoom(roomId1).withStartTime(start.plusHours(4)).build());
            reservationRepository.save(anOneHourReservation().withRoom(roomId1).withStartTime(start.plusHours(2)).build());
            reservationRepository.save(anOneHourReservation().withRoom(roomId2).withStartTime(start.plusHours(4)).build());
            reservationRepository.save(anOneHourReservation().withRoom(roomId2).withStartTime(start.plusHours(2)).build());
            
            // When
            Page<Reservation> page = reservationRepository.findByRoomId(roomId1, PageRequest.of(0, 10, Sort.by("startTime").ascending()));
            
            // Then
            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getTotalPages()).isEqualTo(1);
            assertThat(page.getContent()).allMatch(r -> r.getRoomId().equals(roomId1));
            
            assertThat(page.getContent())
                    .extracting(Reservation::getStartTime)
                    .isSorted();
        }
        
    }

    @Test
    @DisplayName("Should delete reservation by id")
    void shouldDeleteReservationById() {
        // Given
        Reservation reservation = anOneHourReservation().withRoom(roomId1).build();
        Reservation saved = reservationRepository.save(reservation);
        Long id = saved.getId();
        
        // When
        reservationRepository.deleteById(id);
        
        // Then
        assertThat(reservationRepository.findById(id)).isEmpty();
    }
}
