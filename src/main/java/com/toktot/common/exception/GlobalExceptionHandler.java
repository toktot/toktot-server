package com.toktot.common.exception;

import com.toktot.web.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ToktotException.class)
    public ResponseEntity<ApiResponse<Void>> handleToktotException(ToktotException e) {
        log.warn("비즈니스 에러 발생: [{}] {}", e.getErrorCodeName(), e.getMessage());

        return ResponseEntity.ok(
                ApiResponse.error(e.getErrorCode(), e.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().get(0);
        String errorMessage = fieldError.getDefaultMessage();
        String fieldName = fieldError.getField();

        log.warn("Validation 에러: 필드[{}] - {}", fieldName, errorMessage);

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.INVALID_INPUT, errorMessage)
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        String message = String.format("필수 파라미터가 누락되었습니다: %s", e.getParameterName());

        log.warn("파라미터 누락: {}", e.getParameterName());

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.MISSING_REQUIRED_FIELD, message)
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("파라미터 타입이 올바르지 않습니다: %s", e.getName());

        log.warn("파라미터 타입 오류: {} (기대값: {}, 실제값: {})",
                e.getName(), e.getRequiredType(), e.getValue());

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.INVALID_FORMAT, message)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        String message = e.getMessage();

        if (message != null) {
            if (message.contains("Required request body is missing")) {
                log.warn("요청 본문 누락: {}", e.getMessage());
                return ResponseEntity.ok(
                        ApiResponse.error(ErrorCode.MISSING_REQUIRED_FIELD, "요청 본문이 필요합니다.")
                );
            }

            if (message.contains("Cannot deserialize") || message.contains("missing")) {
                log.warn("필수 필드 누락: {}", e.getMessage());
                return ResponseEntity.ok(
                        ApiResponse.error(ErrorCode.MISSING_REQUIRED_FIELD, "필수 필드가 누락되었습니다.")
                );
            }
        }

        log.warn("JSON 파싱 에러: {}", e.getMessage());
        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.INVALID_FORMAT, "요청 형식이 올바르지 않습니다.")
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("잘못된 인수: {}", e.getMessage());

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.INVALID_INPUT)
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
        log.warn("잘못된 상태: {}", e.getMessage());

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.OPERATION_NOT_ALLOWED)
        );
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException e) {
        log.warn("404 에러: {} {}", e.getHttpMethod(), e.getRequestURL());

        return ResponseEntity.status(404).body(
                ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleServerError(Exception e) {
        log.error("예상치 못한 서버 에러 발생", e);

        return ResponseEntity.status(500).body(
                ApiResponse.error(ErrorCode.SERVER_ERROR)
        );
    }

}
