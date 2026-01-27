package com.pinkkila.roomreservationapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SortWhitelistValidator.class)
public @interface SortWhitelist {
    String[] value();
    String message() default "Invalid sort field. Allowed fields are: {value}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
