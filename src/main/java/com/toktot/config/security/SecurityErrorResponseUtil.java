package com.toktot.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toktot.common.exception.ErrorCode;
import com.toktot.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseUtil {

    private final ObjectMapper objectMapper;

    public void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        writeErrorResponse(response, errorCode, errorCode.getMessage());
    }

    public void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode, String customMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Void> errorResponse = ApiResponse.error(errorCode, customMessage);
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();

        log.debug("Security error response written - ErrorCode: {}, Message: {}",
                errorCode.name(), customMessage);
    }
}
