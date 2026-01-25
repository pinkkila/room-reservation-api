package com.pinkkila.roomreservationapi.reservation.validation;

import com.pinkkila.roomreservationapi.reservation.ReservationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ReservationRangeValidator implements ConstraintValidator<ValidReservationRange, ReservationRequest> {

    @Override
    public boolean isValid(ReservationRequest request, ConstraintValidatorContext context) {
        if (request == null || request.startTime() == null || request.endTime() == null) {
            return true;
        }
        return request.startTime().isBefore(request.endTime());
    }
}
