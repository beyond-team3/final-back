package com.monsoon.seedflowplus.core.common.support.error;

import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

public enum ErrorType {

    // 기본 에러 발생
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "예기치 않은 오류가 발생했습니다.", LogLevel.ERROR),

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, ErrorCode.C002, "잘못된 입력값입니다.", LogLevel.WARN),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.C003, "지원하지 않는 HTTP 메서드입니다.", LogLevel.WARN),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, ErrorCode.C004, "잘못된 타입입니다.", LogLevel.WARN),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, ErrorCode.C005, "필수 요청 파라미터가 누락되었습니다.", LogLevel.WARN),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, ErrorCode.A001, "인증이 필요합니다.", LogLevel.WARN),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, ErrorCode.A002, "접근이 거부되었습니다.", LogLevel.WARN),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, ErrorCode.A003, "세션이 만료되었습니다.", LogLevel.WARN),

    ;

    private final HttpStatus status;

    private final ErrorCode code;

    private final String message;

    private final LogLevel logLevel;

    ErrorType(HttpStatus status, ErrorCode code, String message, LogLevel logLevel) {

        this.status = status;
        this.code = code;
        this.message = message;
        this.logLevel = logLevel;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ErrorCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

}
