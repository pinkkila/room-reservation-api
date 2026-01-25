package com.pinkkila.roomreservationapi.reservation;

import com.pinkkila.roomreservationapi.TestcontainersConfiguration;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;

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

    @Test
    @DisplayName("Should save a valid reservation")
    void shouldSaveValidReservation() {
        Reservation reservation = anOneHourReservation().withRoom(roomId1).build();

        Reservation saved = reservationRepository.save(reservation);

        assertThat(saved.getId()).isNotNull();
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
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Nested
    @DisplayName("Finding Reservations with Pagination and Filtering")
    class FindingReservations {

        @Test
        @DisplayName("Should find all reservations with pagination")
        void shouldFindAllWithPagination() {
            // Given
            saveReservations(roomId1, 5);

            // When
            Page<Reservation> page = reservationRepository.findAll(PageRequest.of(0, 3, Sort.by("id").ascending()));

            // Then
            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should find reservations by roomId with pagination")
        void shouldFindByRoomIdWithPagination() {
            // Given
            saveReservations(roomId1, 3);
            saveReservations(roomId2, 2);

            // When
            Page<Reservation> page = reservationRepository.findByRoomId(roomId1, PageRequest.of(0, 10));

            // Then
            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getContent()).allMatch(r -> r.getRoomId().equals(roomId1));
        }

        private void saveReservations(int roomId, int count) {
            OffsetDateTime now = OffsetDateTime.now();
            for (int i = 0; i < count; i++) {
                reservationRepository.save(anOneHourReservation()
                        .withRoom(roomId)
                        .withStartTime(now.plusDays(roomId).plusHours(i * 2))
                        .build());
            }
        }
    }
}
