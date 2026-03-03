package com.monsoon.seedflowplus.core.common.support.error;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ErrorCodeRuntimeException.class)
    protected ResponseEntity<ApiResult<?>> handleErrorCodeException(ErrorCodeRuntimeException e) {
        log.warn("ErrorCodeException: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCodeProvider().getHttpStatus())
                .body(ApiResult.error(e.getErrorCodeProvider(), e.getData()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ApiResult<?>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorType.ACCESS_DENIED.getStatus())
                .body(ApiResult.error(ErrorType.ACCESS_DENIED));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResult<?>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.warn("MethodArgumentNotValidException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorType.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResult.error(ErrorType.INVALID_INPUT_VALUE));
    }

    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ApiResult<?>> handleBindException(BindException e) {
        log.warn("BindException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorType.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResult.error(ErrorType.INVALID_INPUT_VALUE));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ApiResult<?>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorType.INVALID_TYPE_VALUE.getStatus())
                .body(ApiResult.error(ErrorType.INVALID_TYPE_VALUE));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<ApiResult<?>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        log.warn("MissingServletRequestParameterException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorType.MISSING_REQUEST_PARAMETER.getStatus())
                .body(ApiResult.error(ErrorType.MISSING_REQUEST_PARAMETER));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ApiResult<?>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("ConstraintViolationException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorType.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResult.error(ErrorType.INVALID_INPUT_VALUE));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResult<?>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        log.warn("HttpRequestMethodNotSupportedException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorType.METHOD_NOT_ALLOWED.getStatus())
                .body(ApiResult.error(ErrorType.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    protected ResponseEntity<ApiResult<?>> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e) {
        log.warn("HttpMediaTypeNotSupportedException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorType.UNSUPPORTED_MEDIA_TYPE.getStatus())
                .body(ApiResult.error(ErrorType.UNSUPPORTED_MEDIA_TYPE));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResult<?>> handleException(Exception e) {
        log.error("Exception: ", e);
        return ResponseEntity
                .status(ErrorType.DEFAULT_ERROR.getStatus())
                .body(ApiResult.error(ErrorType.DEFAULT_ERROR));
    }
}
