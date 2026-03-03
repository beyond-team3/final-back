package com.monsoon.seedflowplus.domain.schedule.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

public class ValidTimeRangeValidator implements ConstraintValidator<ValidTimeRange, Object> {

    private String startField;
    private String endField;

    @Override
    public void initialize(ValidTimeRange constraintAnnotation) {
        this.startField = constraintAnnotation.startField();
        this.endField = constraintAnnotation.endField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            Method startGetter = value.getClass().getMethod(startField);
            Method endGetter = value.getClass().getMethod(endField);
            Object start = startGetter.invoke(value);
            Object end = endGetter.invoke(value);

            if (!(start instanceof LocalDateTime startAt) || !(end instanceof LocalDateTime endAt)) {
                return false;
            }
            return endAt.isAfter(startAt);
        } catch (Exception e) {
            return false;
        }
    }
}
