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
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.U001, "존재하지 않는 사용자입니다.", LogLevel.WARN),
    DUPLICATE_LOGIN_ID(HttpStatus.BAD_REQUEST, ErrorCode.U002, "이미 존재하는 아이디입니다.", LogLevel.WARN),

    DUPLICATE_PRODUCT_CODE(HttpStatus.BAD_REQUEST, ErrorCode.P001, "이미 존재하는 상품입니다.", LogLevel.WARN),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.P002, "존재하지 않는 상품입니다.", LogLevel.WARN),
    DUPLICATE_TAG(HttpStatus.BAD_REQUEST, ErrorCode.T001, "이미 존재하는 태그입니다.", LogLevel.WARN),

    // 주문
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.O001, "주문을 찾을 수 없습니다.", LogLevel.WARN),
    ORDER_QUANTITY_EXCEEDED(HttpStatus.BAD_REQUEST, ErrorCode.O002, "계약 수량을 초과했습니다.", LogLevel.WARN),
    CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.O003, "계약을 찾을 수 없습니다.", LogLevel.WARN),
    CONTRACT_EXPIRED(HttpStatus.BAD_REQUEST, ErrorCode.O004, "계약 기간이 아닙니다.", LogLevel.WARN),
    CONTRACT_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.O005, "계약 상세를 찾을 수 없습니다.", LogLevel.WARN),
    ORDER_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.O006, "주문 상세를 찾을 수 없습니다.", LogLevel.WARN),
    ORDER_ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, ErrorCode.O007, "이미 확정된 주문입니다.", LogLevel.WARN),

    // 명세서
    STATEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.S001, "명세서를 찾을 수 없습니다.", LogLevel.WARN),
    STATEMENT_ALREADY_ISSUED(HttpStatus.BAD_REQUEST, ErrorCode.S002, "이미 발급된 명세서가 존재합니다.", LogLevel.WARN),

    // 청구서
    INVOICE_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.I001, "청구서를 찾을 수 없습니다.", LogLevel.WARN),
    INVOICE_ALREADY_PUBLISHED(HttpStatus.BAD_REQUEST, ErrorCode.I002, "이미 발행된 청구서입니다.", LogLevel.WARN),
    INVOICE_NOT_PUBLISHED(HttpStatus.BAD_REQUEST, ErrorCode.I003, "발행되지 않은 청구서는 결제할 수 없습니다.", LogLevel.WARN),
    INVOICE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, ErrorCode.I004, "이미 발행 대기 중인 청구서가 있습니다.", LogLevel.WARN),

    // 결제
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.P101, "결제를 찾을 수 없습니다.", LogLevel.WARN),
    ALREADY_PAID(HttpStatus.BAD_REQUEST, ErrorCode.P102, "이미 결제된 청구서입니다.", LogLevel.WARN),

    // 계정
    CLIENT_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.A101, "거래처를 찾을 수 없습니다.", LogLevel.WARN),
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.A102, "영업사원을 찾을 수 없습니다.", LogLevel.WARN),
    DUPLICATE_CLIENT_BRN(HttpStatus.BAD_REQUEST, ErrorCode.U003, "이미 등록된 사업자번호입니다.", LogLevel.WARN),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, ErrorCode.A004, "비활성화된 계정입니다. 관리자에게 문의하세요.", LogLevel.WARN),
    EMPLOYEE_NOT_LINKED(HttpStatus.FORBIDDEN, ErrorCode.A005, "계정에 연결된 직원 정보가 없습니다. 관리자에게 문의하세요.", LogLevel.WARN),
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
