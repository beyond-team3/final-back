package com.monsoon.seedflowplus.domain.deal.common.error;

import com.monsoon.seedflowplus.core.common.support.error.ErrorCodeProvider;
import org.springframework.http.HttpStatus;

public enum DealErrorCode implements ErrorCodeProvider {

    FROM_STATUS_REQUIRED(HttpStatus.BAD_REQUEST, ErrorType.VALIDATION_ERROR, "fromStatus는 null 또는 공백일 수 없습니다."),
    TO_STATUS_REQUIRED(HttpStatus.BAD_REQUEST, ErrorType.VALIDATION_ERROR, "toStatus는 null 또는 공백일 수 없습니다."),
    FIELD_NAME_REQUIRED(HttpStatus.BAD_REQUEST, ErrorType.VALIDATION_ERROR, "fieldName은 null이 될 수 없습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, ErrorType.VALIDATION_ERROR, "fromAt은 toAt보다 늦을 수 없습니다."),

    DEAL_CLIENT_MISMATCH(HttpStatus.BAD_REQUEST, ErrorType.BUSINESS_RULE_VIOLATION, "client는 deal.getClient()와 같아야 합니다."),
    SYSTEM_ACTOR_ID_MUST_BE_NULL(HttpStatus.BAD_REQUEST, ErrorType.BUSINESS_RULE_VIOLATION, "actorType이 SYSTEM이면 actorId는 null이어야 합니다."),
    NON_SYSTEM_ACTOR_ID_REQUIRED(HttpStatus.BAD_REQUEST, ErrorType.BUSINESS_RULE_VIOLATION, "actorType이 SYSTEM이 아니면 actorId는 null일 수 없습니다."),
    CONVERT_ACTION_AT_MISMATCH(HttpStatus.BAD_REQUEST, ErrorType.BUSINESS_RULE_VIOLATION, "original.actionAt과 created.actionAt이 일치해야 합니다."),
    CONVERT_ACTOR_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, ErrorType.BUSINESS_RULE_VIOLATION, "original.actorType과 created.actorType이 일치해야 합니다."),
    CONVERT_ACTOR_ID_MISMATCH(HttpStatus.BAD_REQUEST, ErrorType.BUSINESS_RULE_VIOLATION, "original.actorId와 created.actorId가 일치해야 합니다."),
    INVALID_DOCUMENT_STATUS(HttpStatus.BAD_REQUEST, ErrorType.BUSINESS_RULE_VIOLATION, "DocumentStatus 값이 유효하지 않습니다.");

    private final HttpStatus httpStatus;
    private final ErrorType errorType;
    private final String message;

    DealErrorCode(HttpStatus httpStatus, ErrorType errorType, String message) {
        this.httpStatus = httpStatus;
        this.errorType = errorType;
        this.message = message;
    }

    @Override
    public DealErrorCode getCode() {
        return this;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }
}
