package com.monsoon.seedflowplus.domain.schedule.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidTimeRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTimeRange {

    String message() default "endAt must be after startAt";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String startField() default "startAt";

    String endField() default "endAt";
}
