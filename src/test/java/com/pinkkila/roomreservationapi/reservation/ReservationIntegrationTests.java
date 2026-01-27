package com.pinkkila.roomreservationapi.reservation;

import com.pinkkila.roomreservationapi.TestcontainersConfiguration;
import com.pinkkila.roomreservationapi.room.Room;
import com.pinkkila.roomreservationapi.room.RoomRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.OffsetDateTime;

import static com.pinkkila.roomreservationapi.testdata.ReservationTestData.anOneHourReservation;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Reservation Integration Tests")
class ReservationIntegrationTests {

    @Autowired
    private WebTestClient webTestClient;

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

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/reservations")
    class CreateReservation {

        @Test
        @DisplayName("should create a reservation")
        void shouldCreateReservation() {
            ReservationRequest request = new ReservationRequest(
                    roomId1,
                    OffsetDateTime.now().plusDays(1).withNano(0),
                    OffsetDateTime.now().plusDays(1).plusHours(1).withNano(0)
            );

            webTestClient.post()
                    .uri("/api/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(ReservationResponse.class)
                    .value(response -> {
                        assertThat(response.id()).isNotNull();
                        assertThat(response.roomId()).isEqualTo(roomId1);
                        assertThat(response.startTime()).isEqualTo(request.startTime());
                        assertThat(response.endTime()).isEqualTo(request.endTime());
                    });
        }

        @Test
        @DisplayName("should return 409 when reservations overlap")
        void shouldReturn409WhenOverlapping() {
            OffsetDateTime start = OffsetDateTime.now().plusDays(2).withNano(0);
            OffsetDateTime end = start.plusHours(1);

            ReservationRequest request1 = new ReservationRequest(roomId1, start, end);
            ReservationRequest request2 = new ReservationRequest(roomId1, start.plusMinutes(30), end.plusMinutes(30));

            // First reservation via repository
            reservationRepository.save(anOneHourReservation()
                    .withRoom(roomId1)
                    .withStartTime(start)
                    .withEndTime(end)
                    .build());

            // Second overlapping reservation via API
            webTestClient.post()
                    .uri("/api/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request2)
                    .exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody()
                    .jsonPath("$.title").isEqualTo("Overlapping Reservation");
        }

        @Test
        @DisplayName("should return 404 when room does not exist")
        void shouldReturn404WhenRoomNotFound() {
            ReservationRequest request = new ReservationRequest(
                    999,
                    OffsetDateTime.now().plusDays(1).withNano(0),
                    OffsetDateTime.now().plusDays(1).plusHours(1).withNano(0)
            );

            webTestClient.post()
                    .uri("/api/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.title").isEqualTo("Room Not Found");
        }
    }

    @Nested
    @DisplayName("GET /api/reservations")
    class GetReservations {

        @Test
        @DisplayName("should return paginated reservations")
        void shouldReturnPaginatedReservations() {
            // Given: Create 3 reservations directly in DB
            reservationRepository.save(anOneHourReservation().withRoom(roomId1).withStartTime(OffsetDateTime.now().plusDays(1).withNano(0)).build());
            reservationRepository.save(anOneHourReservation().withRoom(roomId1).withStartTime(OffsetDateTime.now().plusDays(2).withNano(0)).build());
            reservationRepository.save(anOneHourReservation().withRoom(roomId2).withStartTime(OffsetDateTime.now().plusDays(3).withNano(0)).build());

            // When
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/reservations")
                            .queryParam("size", "2")
                            .queryParam("sortBy", "startTime")
                            .queryParam("sortOrder", "asc")
                            .build())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content.length()").isEqualTo(2)
                    .jsonPath("$.page.totalElements").isEqualTo(3)
                    .jsonPath("$.page.totalPages").isEqualTo(2)
                    .jsonPath("$.page.number").isEqualTo(0)
                    .jsonPath("$.page.size").isEqualTo(2);
        }

        @Test
        @DisplayName("should filter by roomId")
        void shouldFilterByRoomId() {
            // Given
            reservationRepository.save(anOneHourReservation().withRoom(roomId1).withStartTime(OffsetDateTime.now().plusDays(1).withNano(0)).build());
            reservationRepository.save(anOneHourReservation().withRoom(roomId2).withStartTime(OffsetDateTime.now().plusDays(2).withNano(0)).build());

            // When
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/reservations")
                            .queryParam("roomId", roomId1.toString())
                            .build())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content.length()").isEqualTo(1)
                    .jsonPath("$.content[0].roomId").isEqualTo(roomId1);
        }

        @Test
        @DisplayName("should return 404 when roomId does not exist")
        void shouldReturn404WhenRoomNotFound() {
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/reservations")
                            .queryParam("roomId", "999")
                            .build())
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.title").isEqualTo("Room Not Found");
        }

        @Test
        @DisplayName("should return 400 when sortBy is invalid")
        void shouldReturn400WhenSortByInvalid() {
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/reservations")
                            .queryParam("sortBy", "secret_column")
                            .build())
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Nested
    @DisplayName("DELETE /api/reservations/{reservationId}")
    class DeleteReservation {

        @Test
        @DisplayName("should delete reservation and return 204")
        void shouldDeleteReservation() {
            // Given: Create a reservation first
            Reservation saved = reservationRepository.save(anOneHourReservation().withRoom(roomId1).build());
            Long reservationId = saved.getId();

            // When: Delete it
            webTestClient.delete()
                    .uri("/api/reservations/{id}", reservationId)
                    .exchange()
                    .expectStatus().isNoContent();

            // Then: Verify it's gone
            webTestClient.get()
                    .uri("/api/reservations")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.page.totalElements").isEqualTo(0);
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() {
            webTestClient.delete()
                    .uri("/api/reservations/{id}", 999)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.title").isEqualTo("Reservation Not Found")
                    .jsonPath("$.detail").isEqualTo("Reservation with ID 999 not found");
        }

        @Test
        @DisplayName("should return 400 when ID is invalid type")
        void shouldReturn400WhenIdInvalidType() {
            webTestClient.delete()
                    .uri("/api/reservations/{id}", "abc")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.title").isEqualTo("Invalid Path Parameter");
        }
    }

}
