## Refactor Controller Tests Reference

- Use `ReservationTestData` to create test data
    - Use static import of so that `anOneHourReservation` can be used directly

- Use following assertion allways when possible:

```
.andExpect(content().json(objectMapper.writeValueAsString(response), JsonCompareMode.STRICT))
```