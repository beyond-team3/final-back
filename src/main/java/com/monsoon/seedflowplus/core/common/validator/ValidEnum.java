package com.monsoon.seedflowplus.core.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidEnumValidator.class) // 검증 로직을 담당할 클래스 지정
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEnum {

    String message() default "유효하지 않은 값입니다."; // 기본 에러 메시지

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    Class<? extends java.lang.Enum<?>> enumClass(); // 어떤 Enum 클래스를 검사할지 받는 변수
}
