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
class ReservationFullIntegrationTests {

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
            ReservationRequest request = anOneHourReservation()
                    .withRoom(roomId1)
                    .asRequest();

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

            ReservationRequest request = anOneHourReservation()
                    .withRoom(roomId1)
                    .withStartTime(start.plusMinutes(30))
                    .asRequest();

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
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody()
                    .jsonPath("$.title").isEqualTo("Overlapping Reservation")
                    .jsonPath("$.type").isEqualTo("urn:room-reservation-api:overlapping-reservation")
                    .jsonPath("$.status").isEqualTo(409)
                    .jsonPath("$.detail").isEqualTo("The room is already reserved for the requested time period.");
        }

        @Test
        @DisplayName("should create reservation when they touch (end time = next start time)")
        void shouldCreateReservationWhenTouching() {
            OffsetDateTime start = OffsetDateTime.now().plusDays(2).withNano(0);
            OffsetDateTime end = start.plusHours(1);

            // First reservation
            reservationRepository.save(anOneHourReservation()
                    .withRoom(roomId1)
                    .withStartTime(start)
                    .withEndTime(end)
                    .build());

            // The second reservation starts exactly when the first ends
            ReservationRequest request = anOneHourReservation()
                    .withRoom(roomId1)
                    .withStartTime(end)
                    .asRequest();

            webTestClient.post()
                    .uri("/api/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated();
        }

        @Test
        @DisplayName("should return 404 when room does not exist")
        void shouldReturn404WhenRoomNotFound() {
            ReservationRequest request = anOneHourReservation()
                    .withRoom(999)
                    .asRequest();

            webTestClient.post()
                    .uri("/api/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.title").isEqualTo("Room Not Found")
                    .jsonPath("$.type").isEqualTo("urn:room-reservation-api:room-not-found")
                    .jsonPath("$.status").isEqualTo(404)
                    .jsonPath("$.detail").isEqualTo("Room with ID 999 not found");
        }

        @Nested
        @DisplayName("Validation")
        class Validation {

            @Test
            @DisplayName("should return 400 when start time is in the past")
            void shouldReturn400WhenStartTimeInPast() {
                ReservationRequest request = anOneHourReservation()
                        .withRoom(roomId1)
                        .withStartTime(OffsetDateTime.now().minusDays(1))
                        .asRequest();

                webTestClient.post()
                        .uri("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody()
                        .jsonPath("$.title").isEqualTo("Invalid Request Body")
                        .jsonPath("$.errors.startTime").isEqualTo("Start time must be in the future");
            }

            @Test
            @DisplayName("should return 400 when end time is before start time")
            void shouldReturn400WhenEndTimeBeforeStartTime() {
                OffsetDateTime start = OffsetDateTime.now().plusDays(1);
                ReservationRequest request = anOneHourReservation()
                        .withRoom(roomId1)
                        .withStartTime(start)
                        .withEndTime(start.minusHours(1))
                        .asRequest();

                webTestClient.post()
                        .uri("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody()
                        .jsonPath("$.title").isEqualTo("Invalid Request Body")
                        .jsonPath("$.detail").isEqualTo("The data provided in the request body is invalid.")
                        .jsonPath("$.errors.invalidField").isEqualTo("Start time must be before end time");
            }

            @Test
            @DisplayName("should return 400 when required fields are missing")
            void shouldReturn400WhenFieldsAreMissing() {
                ReservationRequest request = new ReservationRequest(null, null, null);

                webTestClient.post()
                        .uri("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody()
                        .jsonPath("$.title").isEqualTo("Invalid Request Body")
                        .jsonPath("$.errors.roomId").isEqualTo("Room ID is required")
                        .jsonPath("$.errors.startTime").isEqualTo("Start time is required")
                        .jsonPath("$.errors.endTime").isEqualTo("End time is required");
            }
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
        @DisplayName("should return empty list when no reservations found for filter")
        void shouldReturnEmptyListWhenNoReservationsFound() {
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/reservations")
                            .queryParam("roomId", roomId1.toString())
                            .build())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content.length()").isEqualTo(0)
                    .jsonPath("$.page.totalElements").isEqualTo(0);
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
                    .jsonPath("$.title").isEqualTo("Room Not Found")
                    .jsonPath("$.type").isEqualTo("urn:room-reservation-api:room-not-found")
                    .jsonPath("$.status").isEqualTo(404)
                    .jsonPath("$.detail").isEqualTo("Room with ID 999 not found");
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
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.title").isEqualTo("Invalid Request Parameters")
                    .jsonPath("$.type").isEqualTo("urn:room-reservation-api:invalid-request-parameters")
                    .jsonPath("$.status").isEqualTo(400)
                    .jsonPath("$.errors.sortBy").isEqualTo("Invalid sort field. Allowed fields are: [id, startTime, endTime, roomId]");
        }
    }

    @Nested
    @DisplayName("DELETE /api/reservations/{reservationId}")
    class DeleteReservation {

        @Test
        @DisplayName("should delete reservation and return 204")
        void shouldDeleteReservation() {
            // Given: Create two reservations
            Reservation saved1 = reservationRepository.save(anOneHourReservation().withRoom(roomId1).build());
            reservationRepository.save(anOneHourReservation().withRoom(roomId2).build());
            
            Long reservationId = saved1.getId();

            // When: Delete the first one
            webTestClient.delete()
                    .uri("/api/reservations/{id}", reservationId)
                    .exchange()
                    .expectStatus().isNoContent();

            // Then: Verify only one remains
            webTestClient.get()
                    .uri("/api/reservations")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.page.totalElements").isEqualTo(1)
                    .jsonPath("$.content[0].roomId").isEqualTo(roomId2);
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
                    .jsonPath("$.type").isEqualTo("urn:room-reservation-api:reservation-not-found")
                    .jsonPath("$.status").isEqualTo(404)
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
                    .jsonPath("$.title").isEqualTo("Invalid Path Parameter")
                    .jsonPath("$.type").isEqualTo("urn:room-reservation-api:invalid-path-parameter")
                    .jsonPath("$.status").isEqualTo(400);
        }
    }

}
