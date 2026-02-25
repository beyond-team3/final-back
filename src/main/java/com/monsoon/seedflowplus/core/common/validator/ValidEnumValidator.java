package com.monsoon.seedflowplus.core.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidEnumValidator implements ConstraintValidator<ValidEnum, String> {

    private ValidEnum annotation;

    @Override
    public void initialize(ValidEnum constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        // 지정된 Enum 클래스의 모든 값들을 가져와서 하나씩 비교
        Object[] enumValues = this.annotation.enumClass().getEnumConstants();
        if (enumValues != null) {
            for (Object enumValue : enumValues) {
                if (value.equals(enumValue.toString())) {
                    return true; // Enum 안에 값이 존재하면 검증 통과
                }
            }
        }

        return false; // 다 찾아봤는데 없으면 검증 실패 (400에러)
    }
}