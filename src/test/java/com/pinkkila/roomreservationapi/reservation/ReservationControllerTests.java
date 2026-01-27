package com.pinkkila.roomreservationapi.reservation;

import com.pinkkila.roomreservationapi.exception.GlobalExceptionHandler;
import com.pinkkila.roomreservationapi.reservation.exception.ReservationNotFoundException;
import com.pinkkila.roomreservationapi.room.exception.RoomNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ReservationController.class, GlobalExceptionHandler.class})
@DisplayName("Reservation Controller Tests")
class ReservationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @Nested
    @DisplayName("POST /api/reservations")
    class CreateReservation {

        @Test
        @DisplayName("Successful creation should return 201 Created with response body")
        void createReservation_Success_Returns201() throws Exception {
            ReservationResponse response = new ReservationResponse(
                    1L,
                    1,
                    OffsetDateTime.parse("2026-02-21T15:00:00Z"),
                    OffsetDateTime.parse("2026-02-21T16:00:00Z")
            );

            when(reservationService.createReservation(any(ReservationRequest.class)))
                    .thenReturn(response);

            String requestJson = """
                    {
                      "roomId": 1,
                      "startTime": "2026-02-21T15:00:00Z",
                      "endTime": "2026-02-21T16:00:00Z"
                    }
                    """;

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.roomId").value(1))
                    .andExpect(jsonPath("$.startTime").value("2026-02-21T15:00:00Z"))
                    .andExpect(jsonPath("$.endTime").value("2026-02-21T16:00:00Z"));
        }

        @Test
        @DisplayName("Malformed JSON should return 400 with custom errors")
        void createReservation_MalformedJson_Returns400WithErrors() throws Exception {
            String malformedJson = """
                    {
                      "roomId": 1,
                      "startTime": "xxx",
                      "endTime": "2026-02-21T16:00:00Z"
                    }
                    """;

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Request Body"))
                    .andExpect(jsonPath("$.detail").value("Malformed or invalid JSON payload"));
        }

        @Test
        @DisplayName("Invalid fields should return 400 with field errors")
        void createReservation_InvalidFields_Returns400WithFieldErrors() throws Exception {
            String invalidJson = """
                    {
                      "roomId": null,
                      "startTime": "2026-02-21T15:00:00Z",
                      "endTime": "2026-02-21T14:00:00Z"
                    }
                    """;

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Request Body"))
                    .andExpect(jsonPath("$.detail").value("The data provided in the request body is invalid."))
                    .andExpect(jsonPath("$.errors.roomId").exists());
        }

        @Test
        @DisplayName("Reservation with end time before start time should return 400")
        void createReservation_EndTimeBeforeStartTime_Returns400() throws Exception {
            String invalidJson = """
                    {
                      "roomId": 1,
                      "startTime": "2026-02-21T16:00:00Z",
                      "endTime": "2026-02-21T15:00:00Z"
                    }
                    """;

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Request Body"));
        }

        @Test
        @DisplayName("Creation for non-existent room should return 404 Not Found")
        void createReservation_RoomNotFound_Returns404() throws Exception {
            when(reservationService.createReservation(any(ReservationRequest.class)))
                    .thenThrow(new RoomNotFoundException(999));

            String requestJson = """
                    {
                      "roomId": 999,
                      "startTime": "2026-02-21T15:00:00Z",
                      "endTime": "2026-02-21T16:00:00Z"
                    }
                    """;

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Room Not Found"))
                    .andExpect(jsonPath("$.detail").value("Room with ID 999 not found"));
        }

        @Test
        @DisplayName("Data conflict during creation should return 400 Bad Request (generic error)")
        void createReservation_DataConflict_Returns400() throws Exception {
            when(reservationService.createReservation(any(ReservationRequest.class)))
                    .thenThrow(new DataIntegrityViolationException("Conflict"));

            String requestJson = """
                    {
                      "roomId": 1,
                      "startTime": "2026-02-21T15:00:00Z",
                      "endTime": "2026-02-21T16:00:00Z"
                    }
                    """;

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Reservation Error"))
                    .andExpect(jsonPath("$.detail").value("Something went wrong when creating reservation."));
        }
    }

    @Nested
    @DisplayName("GET /api/reservations")
    class GetReservations {

        @Test
        @DisplayName("Should return page of reservations with default values")
        void shouldReturnPageOfReservations() throws Exception {
            ReservationResponse res1 = new ReservationResponse(
                    1L, 1,
                    OffsetDateTime.parse("2026-02-21T15:00:00Z"),
                    OffsetDateTime.parse("2026-02-21T16:00:00Z")
            );
            Page<ReservationResponse> page = new PageImpl<>(List.of(res1), PageRequest.of(0, 20), 1);

            when(reservationService.getReservations(any(ReservationQuery.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/reservations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.page.totalElements").value(1))
                    .andExpect(jsonPath("$.page.size").value(20))
                    .andExpect(jsonPath("$.page.number").value(0));

            ArgumentCaptor<ReservationQuery> captor = ArgumentCaptor.forClass(ReservationQuery.class);
            verify(reservationService).getReservations(captor.capture());

            ReservationQuery query = captor.getValue();
            assertThat(query.page()).isEqualTo(0);
            assertThat(query.size()).isEqualTo(20);
            assertThat(query.sortBy()).isEqualTo("startTime");
            assertThat(query.sortOrder()).isEqualTo("asc");
            assertThat(query.roomId()).isNull();
        }

        @Test
        @DisplayName("Should return reservations filtered by roomId")
        void shouldReturnFilteredReservations() throws Exception {
            ReservationResponse res1 = new ReservationResponse(
                    1L, 10,
                    OffsetDateTime.parse("2026-02-21T15:00:00Z"),
                    OffsetDateTime.parse("2026-02-21T16:00:00Z")
            );
            Page<ReservationResponse> page = new PageImpl<>(List.of(res1));

            when(reservationService.getReservations(any(ReservationQuery.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/reservations")
                            .param("roomId", "10")
                            .param("page", "1")
                            .param("size", "10")
                            .param("sortBy", "endTime")
                            .param("sortOrder", "desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].roomId").value(10));

            ArgumentCaptor<ReservationQuery> captor = ArgumentCaptor.forClass(ReservationQuery.class);
            verify(reservationService).getReservations(captor.capture());

            ReservationQuery query = captor.getValue();
            assertThat(query.roomId()).isEqualTo(10);
            assertThat(query.page()).isEqualTo(1);
            assertThat(query.size()).isEqualTo(10);
            assertThat(query.sortBy()).isEqualTo("endTime");
            assertThat(query.sortOrder()).isEqualTo("desc");
        }

        @Test
        @DisplayName("Should return 400 for invalid query parameter type")
        void shouldReturn400ForInvalidQueryType() throws Exception {
            mockMvc.perform(get("/api/reservations")
                            .param("roomId", "x"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Request Parameters"))
                    .andExpect(jsonPath("$.errors.roomId").exists());
        }

        @Test
        @DisplayName("Should return 400 for invalid query parameter values")
        void shouldReturn400ForInvalidQueryValues() throws Exception {
            mockMvc.perform(get("/api/reservations")
                            .param("page", "-1")
                            .param("size", "0")
                            .param("sortOrder", "invalid")
                            .param("roomId", "-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Request Parameters"))
                    .andExpect(jsonPath("$.errors.page").exists())
                    .andExpect(jsonPath("$.errors.size").exists())
                    .andExpect(jsonPath("$.errors.sortOrder").exists())
                    .andExpect(jsonPath("$.errors.roomId").exists());
        }

        @Test
        @DisplayName("Should return 400 when sortBy field is invalid")
        void shouldReturn400WhenSortByInvalid() throws Exception {
            mockMvc.perform(get("/api/reservations")
                            .param("sortBy", "invalidField"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Request Parameters"))
                    .andExpect(jsonPath("$.errors.sortBy").exists());
        }

        @Test
        @DisplayName("Should return 404 when filtering by non-existent roomId")
        void shouldReturn404WhenRoomNotFound() throws Exception {
            when(reservationService.getReservations(any(ReservationQuery.class)))
                    .thenThrow(new RoomNotFoundException(999));

            mockMvc.perform(get("/api/reservations")
                            .param("roomId", "999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Room Not Found"))
                    .andExpect(jsonPath("$.detail").value("Room with ID 999 not found"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/reservations/{reservationId}")
    class DeleteReservation {

        @Test
        @DisplayName("Should return 204 No Content on successful deletion")
        void shouldReturn204OnSuccess() throws Exception {
            mockMvc.perform(delete("/api/reservations/1"))
                    .andExpect(status().isNoContent());

            verify(reservationService).deleteReservation(1L);
        }

        @Test
        @DisplayName("Should return 404 Not Found when reservation does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            doThrow(new ReservationNotFoundException(99L))
                    .when(reservationService).deleteReservation(99L);

            mockMvc.perform(delete("/api/reservations/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Reservation Not Found"))
                    .andExpect(jsonPath("$.detail").value("Reservation with ID 99 not found"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ID is not a number")
        void shouldReturn400WhenIdNotNumeric() throws Exception {
            mockMvc.perform(delete("/api/reservations/abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Path Parameter"))
                    .andExpect(jsonPath("$.detail").value("One or more path parameters are invalid."));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ID is not positive")
        void shouldReturn400WhenIdNotPositive() throws Exception {
            mockMvc.perform(delete("/api/reservations/0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Path Parameter"))
                    .andExpect(jsonPath("$.detail").value("One or more path parameters are invalid."));
        }
    }
}
