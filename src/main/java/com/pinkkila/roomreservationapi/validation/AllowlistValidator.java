package com.pinkkila.roomreservationapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class AllowlistValidator implements ConstraintValidator<Allowlist, String> {

    private Set<String> allowedFields;

    @Override
    public void initialize(Allowlist constraintAnnotation) {
        this.allowedFields = Set.of(constraintAnnotation.value());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return allowedFields.contains(value);
    }
}
