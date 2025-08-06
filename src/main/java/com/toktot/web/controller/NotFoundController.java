package com.toktot.web.controller;

import com.toktot.common.exception.ErrorCode;
import com.toktot.web.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Order(Integer.MAX_VALUE)
public class NotFoundController {

    @RequestMapping("/**")
    public ResponseEntity<ApiResponse<Void>> handleNotFound(HttpServletRequest request) {
        log.atWarn()
                .setMessage("API not found")
                .addKeyValue("requestUri", request.getRequestURI())
                .addKeyValue("requestMethod", request.getMethod())
                .addKeyValue("userAgent", request.getHeader("User-Agent"))
                .addKeyValue("remoteAddr", request.getRemoteAddr())
                .log();

        return ResponseEntity.ok(ApiResponse.error(ErrorCode.API_NOT_FOUND));
    }
}
