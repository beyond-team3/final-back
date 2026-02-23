package com.monsoon.seedflowplus.core.common.support.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        response.setStatus(ErrorType.ACCESS_DENIED.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        ApiResult<?> errorResponse = ApiResult.error(ErrorType.ACCESS_DENIED);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
