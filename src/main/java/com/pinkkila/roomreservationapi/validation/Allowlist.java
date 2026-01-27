package com.pinkkila.roomreservationapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AllowlistValidator.class)
public @interface Allowlist {
    String[] value();
    String message() default "Value is not allowed. Allowed values are: {value}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
