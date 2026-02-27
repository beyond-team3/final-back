package com.monsoon.seedflowplus.core.common.support.response;

import com.monsoon.seedflowplus.core.common.support.error.ErrorMessage;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.deal.common.error.DealErrorCode;
import lombok.Getter;

@Getter
public class ApiResult<S> {
    private final ResultType result;
    private final S data;
    private final ErrorMessage error;

    private ApiResult(ResultType result, S data, ErrorMessage error) {
        this.result = result;
        this.data = data;
        this.error = error;
    }

    public static ApiResult<?> success() {
        return new ApiResult<>(ResultType.SUCCESS, null, null);
    }

    public static <S> ApiResult<S> success(S data) {
        return new ApiResult<>(ResultType.SUCCESS, data, null);
    }

    public static ApiResult<?> error(ErrorType error) {
        return new ApiResult<>(ResultType.ERROR, null, new ErrorMessage(error));
    }

    public static ApiResult<?> error(ErrorType error, Object errorData) {
        return new ApiResult<>(ResultType.ERROR, null, new ErrorMessage(error, errorData));
    }

    public static ApiResult<?> error(DealErrorCode errorCode) {
        return new ApiResult<>(ResultType.ERROR, null, new ErrorMessage(errorCode));
    }

    public static ApiResult<?> error(
            DealErrorCode errorCode,
            Object errorData
    ) {
        return new ApiResult<>(ResultType.ERROR, null, new ErrorMessage(errorCode, errorData));
    }
}
