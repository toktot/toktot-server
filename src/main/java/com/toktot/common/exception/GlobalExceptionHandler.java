package com.toktot.common.exception;

import com.toktot.web.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ToktotException.class)
    public ResponseEntity<ApiResponse<Void>> handleToktotException(ToktotException e, HttpServletRequest request) {
        log.atWarn()
                .setMessage("Business error occurred")
                .addKeyValue("errorCode", e.getErrorCodeName())
                .addKeyValue("errorMessage", e.getMessage())
                .addKeyValue("requestUri", request.getRequestURI())
                .addKeyValue("requestMethod", request.getMethod())
                .addKeyValue("isAuthError", e.isAuthError())
                .addKeyValue("isValidationError", e.isValidationError())
                .addKeyValue("isPermissionError", e.isPermissionError())
                .addKeyValue("isSystemError", e.isSystemError())
                .log();

        return ResponseEntity.ok(
                ApiResponse.error(e.getErrorCode(), e.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().get(0);
        String errorMessage = fieldError.getDefaultMessage();
        String fieldName = fieldError.getField();
        Object rejectedValue = fieldError.getRejectedValue();

        log.atWarn()
                .setMessage("Validation error occurred")
                .addKeyValue("fieldName", fieldName)
                .addKeyValue("errorMessage", errorMessage)
                .addKeyValue("rejectedValue", rejectedValue != null ? rejectedValue.toString() : "null")
                .addKeyValue("requestUri", request.getRequestURI())
                .addKeyValue("requestMethod", request.getMethod())
                .log();

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.INVALID_INPUT, errorMessage)
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e, HttpServletRequest request) {
        String message = String.format("필수 파라미터가 누락되었습니다: %s", e.getParameterName());

        log.atWarn()
                .setMessage("Missing parameter error")
                .addKeyValue("parameterName", e.getParameterName())
                .addKeyValue("parameterType", e.getParameterType())
                .addKeyValue("requestUri", request.getRequestURI())
                .addKeyValue("requestMethod", request.getMethod())
                .log();

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.MISSING_REQUIRED_FIELD, message)
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = String.format("파라미터 타입이 올바르지 않습니다: %s", e.getName());

        log.atWarn()
                .setMessage("Parameter type mismatch error")
                .addKeyValue("parameterName", e.getName())
                .addKeyValue("expectedType", e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown")
                .addKeyValue("actualValue", e.getValue())
                .addKeyValue("requestUri", request.getRequestURI())
                .addKeyValue("requestMethod", request.getMethod())
                .log();

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.INVALID_FORMAT, message)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        String message = e.getMessage();
        String errorType = "JSON_PARSING_ERROR";

        if (message != null) {
            if (message.contains("Required request body is missing")) {
                errorType = "MISSING_REQUEST_BODY";
                log.atWarn()
                        .setMessage("Request body missing error")
                        .addKeyValue("errorType", errorType)
                        .addKeyValue("requestUri", request.getRequestURI())
                        .addKeyValue("requestMethod", request.getMethod())
                        .log();
                return ResponseEntity.ok(
                        ApiResponse.error(ErrorCode.MISSING_REQUIRED_FIELD, "요청 본문이 필요합니다.")
                );
            }

            if (message.contains("Cannot deserialize value") &&
                    (message.contains("not one of the values accepted for Enum") ||
                            message.contains("value not one of declared Enum"))) {
                errorType = "INVALID_ENUM_VALUE";
                log.atWarn()
                        .setMessage("Invalid enum value error")
                        .addKeyValue("errorType", errorType)
                        .addKeyValue("requestUri", request.getRequestURI())
                        .addKeyValue("requestMethod", request.getMethod())
                        .log();
                return ResponseEntity.ok(
                        ApiResponse.error(ErrorCode.INVALID_INPUT, "유효하지 않은 선택값입니다.")
                );
            }

            if (message.contains("missing") && !message.contains("Cannot deserialize")) {
                errorType = "FIELD_MISSING_ERROR";
                log.atWarn()
                        .setMessage("Field missing error")
                        .addKeyValue("errorType", errorType)
                        .addKeyValue("requestUri", request.getRequestURI())
                        .addKeyValue("requestMethod", request.getMethod())
                        .log();
                return ResponseEntity.ok(
                        ApiResponse.error(ErrorCode.MISSING_REQUIRED_FIELD, "필수 필드가 누락되었습니다.")
                );
            }
        }

        log.atWarn()
                .setMessage("JSON parsing error")
                .addKeyValue("errorType", errorType)
                .addKeyValue("errorMessage", e.getMessage())
                .addKeyValue("requestUri", request.getRequestURI())
                .addKeyValue("requestMethod", request.getMethod())
                .log();

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.INVALID_FORMAT, "요청 형식이 올바르지 않습니다.")
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.FILE_SIZE_EXCEEDED);

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.atWarn()
                .setMessage("Illegal argument error")
                .addKeyValue("errorMessage", e.getMessage())
                .addKeyValue("requestUri", request.getRequestURI())
                .addKeyValue("requestMethod", request.getMethod())
                .log();

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.INVALID_INPUT)
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException e, HttpServletRequest request) {
        log.atWarn()
                .setMessage("Illegal state error")
                .addKeyValue("errorMessage", e.getMessage())
                .addKeyValue("requestUri", request.getRequestURI())
                .addKeyValue("requestMethod", request.getMethod())
                .log();

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.OPERATION_NOT_ALLOWED)
        );
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException e, HttpServletRequest request) {
        log.atWarn()
                .setMessage("404 Not Found error")
                .addKeyValue("requestUri", e.getRequestURL())
                .addKeyValue("httpMethod", e.getHttpMethod())
                .addKeyValue("headers", e.getHeaders().toString())
                .log();

        return ResponseEntity.status(404).body(
                ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleServerError(Exception e, HttpServletRequest request) {
        log.atError()
                .setMessage("Unexpected server error occurred")
                .addKeyValue("errorType", e.getClass().getSimpleName())
                .addKeyValue("errorMessage", e.getMessage())
                .addKeyValue("requestUri", request.getRequestURI())
                .addKeyValue("requestMethod", request.getMethod())
                .addKeyValue("userAgent", request.getHeader("User-Agent"))
                .addKeyValue("remoteAddr", request.getRemoteAddr())
                .setCause(e)
                .log();

        return ResponseEntity.status(500).body(
                ApiResponse.error(ErrorCode.SERVER_ERROR)
        );
    }
}
