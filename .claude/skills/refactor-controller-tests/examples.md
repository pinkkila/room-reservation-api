# Refactor Contoller Tests Examples

If this is the starting point:

```java

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
```

Code should be refactored to this:

```java
import static com.pinkkila.roomreservationapi.testdata.ReservationTestData.anOneHourReservation;

@Autowired
private ObjectMapper objectMapper;

@Test
@DisplayName("Successful creation should return 201 Created with response body")
void createReservation_Success_Returns201() throws Exception {
    ReservationResponse response = anOneHourReservation().withRoom(1).asResponse(1L);
    
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
            .andExpect(content().json(objectMapper.writeValueAsString(response), JsonCompareMode.STRICT));
}
```