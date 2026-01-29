package com.pinkkila.roomreservationapi.reservation;

import com.pinkkila.roomreservationapi.exception.ErrorType;
import com.pinkkila.roomreservationapi.exception.GlobalExceptionHandler;
import com.pinkkila.roomreservationapi.reservation.exception.ReservationNotFoundException;
import com.pinkkila.roomreservationapi.room.exception.RoomNotFoundException;
import com.pinkkila.roomreservationapi.testdata.ReservationTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.relational.core.conversion.DbAction;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
            var reservationBuilder = ReservationTestData.anOneHourReservation().withRoom(1);
            ReservationResponse response = reservationBuilder.asResponse(1L);

            when(reservationService.createReservation(any(ReservationRequest.class)))
                    .thenReturn(response);

            String requestJson = """
                    {
                      "roomId": %d,
                      "startTime": "%s",
                      "endTime": "%s"
                    }
                    """.formatted(response.roomId(), response.startTime(), response.endTime());

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(response.id()))
                    .andExpect(jsonPath("$.roomId").value(response.roomId()))
                    .andExpect(jsonPath("$.startTime").value(response.startTime().toString()))
                    .andExpect(jsonPath("$.endTime").value(response.endTime().toString()));
        }
        
        @Test
        @DisplayName("Overlapping reservation during creation should return 409 Conflict")
        void createReservation_Overlapping_Reservation_Returns409() throws Exception {
            var psqlException = mock(org.postgresql.util.PSQLException.class);
            var serverError = mock(org.postgresql.util.ServerErrorMessage.class);
            
            when(psqlException.getServerErrorMessage()).thenReturn(serverError);
            when(serverError.getConstraint()).thenReturn("reservation_overlap_excl");
            
            var dbActionExecutionException = new DbActionExecutionException(mock(DbAction.class), psqlException);
            
            when(reservationService.createReservation(any(ReservationRequest.class)))
                    .thenThrow(dbActionExecutionException);
            
            var request = ReservationTestData.anOneHourReservation().withRoom(1).asRequest();
            String requestJson = """
                    {
                      "roomId": %d,
                      "startTime": "%s",
                      "endTime": "%s"
                    }
                    """.formatted(request.roomId(), request.startTime(), request.endTime());
            
            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value(ErrorType.OVERLAPPING_RESERVATION.toUrn().toString()))
                    .andExpect(jsonPath("$.title").value(ErrorType.OVERLAPPING_RESERVATION.getTitle()))
                    .andExpect(jsonPath("$.detail").value("The room is already reserved for the requested time period."));
        }
        
        @Test
        @DisplayName("Reservation with end time before start time should return 400 with field errors")
        void createReservation_EndTimeBeforeStartTime_Returns400WithErrors() throws Exception {
            var startTime = OffsetDateTime.now().plusDays(1).withNano(0);
            var request = ReservationTestData.anOneHourReservation()
                    .withRoom(1)
                    .withStartTime(startTime)
                    .withEndTime(startTime.minusHours(1))
                    .asRequest();

            String invalidJson = """
                    {
                      "roomId": %d,
                      "startTime": "%s",
                      "endTime": "%s"
                    }
                    """.formatted(request.roomId(), request.startTime(), request.endTime());
            
            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(ErrorType.INVALID_REQUEST_BODY.toUrn().toString()))
                    .andExpect(jsonPath("$.title").value(ErrorType.INVALID_REQUEST_BODY.getTitle()))
                    .andExpect(jsonPath("$.detail").value("The data provided in the request body is invalid."))
                    .andExpect(jsonPath("$.errors.invalidField").value("Start time must be before end time"));
        }
        
        @Test
        @DisplayName("Reservation with start and end time in past should return 400 with field errors")
        void createReservation_StartTimeEndTimeInPast_Returns400WithErrors() throws Exception {
            var startTime = OffsetDateTime.now().minusDays(1).withNano(0);
            var request = ReservationTestData.anOneHourReservation()
                    .withRoom(1)
                    .withStartTime(startTime)
                    .asRequest();

            String invalidJson = """
                    {
                      "roomId": %d,
                      "startTime": "%s",
                      "endTime": "%s"
                    }
                    """.formatted(request.roomId(), request.startTime(), request.endTime());

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(ErrorType.INVALID_REQUEST_BODY.toUrn().toString()))
                    .andExpect(jsonPath("$.title").value(ErrorType.INVALID_REQUEST_BODY.getTitle()))
                    .andExpect(jsonPath("$.detail").value("The data provided in the request body is invalid."))
                    .andExpect(jsonPath("$.errors.startTime").value("Start time must be in the future"))
                    .andExpect(jsonPath("$.errors.endTime").value("End time must be in the future"));
            
        }
        
        @Test
        @DisplayName("Creation for non-existent room should return 404 Not Found")
        void createReservation_RoomNotFound_Returns404() throws Exception {
            when(reservationService.createReservation(any(ReservationRequest.class)))
                    .thenThrow(new RoomNotFoundException(999));
            
            var request = ReservationTestData.anOneHourReservation().withRoom(999).asRequest();
            String requestJson = """
                    {
                      "roomId": %d,
                      "startTime": "%s",
                      "endTime": "%s"
                    }
                    """.formatted(request.roomId(), request.startTime(), request.endTime());
            
            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value(ErrorType.ROOM_NOT_FOUND.toUrn().toString()))
                    .andExpect(jsonPath("$.title").value(ErrorType.ROOM_NOT_FOUND.getTitle()))
                    .andExpect(jsonPath("$.detail").value("Room with ID 999 not found"));
        }
        
        @Test
        @DisplayName("Malformed JSON should return 400")
        void createReservation_MalformedJson_Returns400WithErrors() throws Exception {
            var request = ReservationTestData.anOneHourReservation().withRoom(1).asRequest();
            String malformedJson = """
                    {
                      "roomId": %d,
                      "startTime": "xxx",
                      "endTime": "%s"
                    }
                    """.formatted(request.roomId(), request.endTime());

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(ErrorType.INVALID_REQUEST_BODY.toUrn().toString()))
                    .andExpect(jsonPath("$.title").value(ErrorType.INVALID_REQUEST_BODY.getTitle()))
                    .andExpect(jsonPath("$.detail").value("Malformed or invalid JSON payload"));
        }
        
        @Test
        @DisplayName("Invalid fields should return 400 with field errors")
        void createReservation_InvalidFields_Returns400WithFieldErrors() throws Exception {
            var startTime = OffsetDateTime.now().plusDays(1).withNano(0);
            var request = ReservationTestData.anOneHourReservation()
                    .withStartTime(startTime)
                    .withEndTime(startTime.minusHours(1))
                    .asRequest();

            String invalidJson = """
                    {
                      "roomId": null,
                      "startTime": "%s",
                      "endTime": "%s"
                    }
                    """.formatted(request.startTime(), request.endTime());

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(ErrorType.INVALID_REQUEST_BODY.toUrn().toString()))
                    .andExpect(jsonPath("$.title").value(ErrorType.INVALID_REQUEST_BODY.getTitle()))
                    .andExpect(jsonPath("$.detail").value("The data provided in the request body is invalid."))
                    .andExpect(jsonPath("$.errors.roomId").value("Room ID is required"));
        }

        @Test
        @DisplayName("Unknown DbActionExecutionException should return 500")
        void createReservation_UnknownDbActionExcecutionException_Returns500() throws Exception {
            var dbActionExecutionException = new DbActionExecutionException(mock(DbAction.class), new RuntimeException("An internal server error occurred."));
            when(reservationService.createReservation(any(ReservationRequest.class)))
                    .thenThrow(dbActionExecutionException);

            var request = ReservationTestData.anOneHourReservation().withRoom(1).asRequest();
            String requestJson = """
                    {
                      "roomId": %d,
                      "startTime": "%s",
                      "endTime": "%s"
                    }
                    """.formatted(request.roomId(), request.startTime(), request.endTime());

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.type").value(ErrorType.INTERNAL_SERVER_ERROR.toUrn().toString()))
                    .andExpect(jsonPath("$.title").value(ErrorType.INTERNAL_SERVER_ERROR.getTitle()))
                    .andExpect(jsonPath("$.detail").value("An internal server error occurred."));
        }
    }

    @Nested
    @DisplayName("GET /api/reservations")
    class GetReservations {

        @Test
        @DisplayName("Should return 200 for page of reservations with default values")
        void getReservations_SuccessDefault_Returns200() throws Exception {
            ReservationResponse res1 = ReservationTestData.anOneHourReservation()
                    .withRoom(1)
                    .asResponse(1L);
            Page<ReservationResponse> page = new PageImpl<>(List.of(res1), PageRequest.of(0, 20), 1);

            when(reservationService.getReservations(any(ReservationQuery.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/reservations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(res1.id()))
                    .andExpect(jsonPath("$.content[0].roomId").value(res1.roomId()))
                    .andExpect(jsonPath("$.content[0].startTime").value(res1.startTime().toString()))
                    .andExpect(jsonPath("$.content[0].endTime").value(res1.endTime().toString()))
                    .andExpect(jsonPath("$.page.totalElements").value(1))
                    .andExpect(jsonPath("$.page.size").value(20))
                    .andExpect(jsonPath("$.page.number").value(0))
                    .andExpect(jsonPath("$.page.totalPages").value(1));

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
        @DisplayName("Should return 200 for reservations filtered by roomId in second page")
        void getReservations_SuccessWithFiltes_Returns200() throws Exception {
            ReservationResponse res1 = ReservationTestData.anOneHourReservation()
                    .withRoom(10)
                    .asResponse(1L);
            Page<ReservationResponse> page = new PageImpl<>(List.of(res1), PageRequest.of(1, 10), 11);

            when(reservationService.getReservations(any(ReservationQuery.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/reservations")
                            .param("roomId", "10")
                            .param("page", "1")
                            .param("size", "10")
                            .param("sortBy", "endTime")
                            .param("sortOrder", "desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(res1.id()))
                    .andExpect(jsonPath("$.content[0].roomId").value(res1.roomId()))
                    .andExpect(jsonPath("$.content[0].startTime").value(res1.startTime().toString()))
                    .andExpect(jsonPath("$.content[0].endTime").value(res1.endTime().toString()))
                    .andExpect(jsonPath("$.page.totalElements").value(11))
                    .andExpect(jsonPath("$.page.size").value(10))
                    .andExpect(jsonPath("$.page.number").value(1))
                    .andExpect(jsonPath("$.page.totalPages").value(2));

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
        void getReservation_InvalidQueryType_Returns400() throws Exception {
            mockMvc.perform(get("/api/reservations")
                            .param("roomId", "x"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(ErrorType.INVALID_REQUEST_PARAMETERS.toUrn().toString()))
                    .andExpect(jsonPath("$.title").value(ErrorType.INVALID_REQUEST_PARAMETERS.getTitle()))
                    .andExpect(jsonPath("$.detail").value("One or more request parameters provided in the URL are invalid."))
                    .andExpect(jsonPath("$.errors.roomId").value("Invalid value"));
        }

        @Test
        @DisplayName("Should return 400 for invalid query parameter values")
        void getReservations_InvalidQueryValues_Returns400() throws Exception {
            mockMvc.perform(get("/api/reservations")
                            .param("page", "-1")
                            .param("size", "0")
                            .param("sortOrder", "invalid")
                            .param("roomId", "-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(ErrorType.INVALID_REQUEST_PARAMETERS.toUrn().toString()))
                    .andExpect(jsonPath("$.title").value(ErrorType.INVALID_REQUEST_PARAMETERS.getTitle()))
                    .andExpect(jsonPath("$.detail").value("One or more request parameters provided in the URL are invalid."))
                    .andExpect(jsonPath("$.errors.page").value("must be greater than or equal to 0"))
                    .andExpect(jsonPath("$.errors.size").value("must be greater than or equal to 1"))
                    .andExpect(jsonPath("$.errors.sortOrder").value("Sort order must be 'asc' or 'desc'"))
                    .andExpect(jsonPath("$.errors.roomId").value("must be greater than 0"));
        }

        @Test
        @DisplayName("Should return 400 when sortBy field is invalid")
        void getReservations_SortByInvalid_Returns400() throws Exception {
            mockMvc.perform(get("/api/reservations")
                            .param("sortBy", "invalidField"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(ErrorType.INVALID_REQUEST_PARAMETERS.toUrn().toString()))
                    .andExpect(jsonPath("$.title").value(ErrorType.INVALID_REQUEST_PARAMETERS.getTitle()))
                    .andExpect(jsonPath("$.detail").value("One or more request parameters provided in the URL are invalid."))
                    .andExpect(jsonPath("$.errors.sortBy").value("Invalid sort field. Allowed fields are: [id, startTime, endTime, roomId]"));
        }

        @Test
        @DisplayName("Should return 404 when filtering by non-existent roomId")
        void getReservations_RoomNotFound_Returns404() throws Exception {
            when(reservationService.getReservations(any(ReservationQuery.class)))
                    .thenThrow(new RoomNotFoundException(999));

            mockMvc.perform(get("/api/reservations")
                            .param("roomId", "999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value(ErrorType.ROOM_NOT_FOUND.toUrn().toString()))
                    .andExpect(jsonPath("$.title").value(ErrorType.ROOM_NOT_FOUND.getTitle()))
                    .andExpect(jsonPath("$.detail").value("Room with ID 999 not found"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/reservations/{reservationId}")
    class DeleteReservation {

        @Test
        @DisplayName("Should return 204 No Content on successful deletion")
        void deleteReservation_Success_Returns204() throws Exception {
            mockMvc.perform(delete("/api/reservations/1"))
                    .andExpect(status().isNoContent());

            verify(reservationService).deleteReservation(1L);
        }

        @Test
        @DisplayName("Should return 404 Not Found when reservation does not exist")
        void deleteReservation_ReservationNotFound_Returns404() throws Exception {
            doThrow(new ReservationNotFoundException(99L))
                    .when(reservationService).deleteReservation(99L);

            mockMvc.perform(delete("/api/reservations/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value(ErrorType.RESERVATION_NOT_FOUND.toUrn().toString()))
                    .andExpect(jsonPath("$.title").value(ErrorType.RESERVATION_NOT_FOUND.getTitle()))
                    .andExpect(jsonPath("$.detail").value("Reservation with ID 99 not found"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ID is not a number")
        void deleteReservation_IdNotNumeric_Returns400() throws Exception {
            mockMvc.perform(delete("/api/reservations/abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(ErrorType.INVALID_PATH_PARAMETER.toUrn().toString()))
                    .andExpect(jsonPath("$.title").value(ErrorType.INVALID_PATH_PARAMETER.getTitle()))
                    .andExpect(jsonPath("$.detail").value("One or more path parameters are invalid."));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ID is not positive")
        void deleteReservation_IdNotPositive_Returns400() throws Exception {
            mockMvc.perform(delete("/api/reservations/0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(ErrorType.INVALID_PATH_PARAMETER.toUrn().toString()))
                    .andExpect(jsonPath("$.title").value(ErrorType.INVALID_PATH_PARAMETER.getTitle()))
                    .andExpect(jsonPath("$.detail").value("One or more path parameters are invalid."));
        }
    }
}
