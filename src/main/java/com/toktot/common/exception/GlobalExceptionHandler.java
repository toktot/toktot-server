package com.toktot.common.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

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
                .log();

        return ResponseEntity.ok(
                ApiResponse.error(e.getErrorCode(), e.getMessage())
        );
    }

    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleS3Exception(S3Exception e) {
        log.error("S3 service error occurred - errorCode: {}, statusCode: {}",
                e.awsErrorDetails().errorCode(), e.statusCode(), e);

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.EXTERNAL_SERVICE_ERROR, "이미지 저장에 실패했습니다.")
        );
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<Void>> handleIOException(IOException e) {
        log.error("File IO error occurred", e);

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.FILE_UPLOAD_FAILED, "파일 처리에 실패했습니다.")
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().get(0);
        String errorMessage = fieldError.getDefaultMessage();

        log.atWarn()
                .setMessage("Validation error occurred")
                .addKeyValue("fieldName", fieldError.getField())
                .addKeyValue("errorMessage", errorMessage)
                .addKeyValue("requestUri", request.getRequestURI())
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
                .addKeyValue("requestUri", request.getRequestURI())
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
                .addKeyValue("requestUri", request.getRequestURI())
                .log();

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.INVALID_FORMAT, message)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        String message = e.getMessage();

        if (message != null) {
            if (message.contains("Required request body is missing")) {
                return ResponseEntity.ok(
                        ApiResponse.error(ErrorCode.MISSING_REQUIRED_FIELD, "요청 본문이 필요합니다.")
                );
            }
            if (message.contains("not one of the values accepted for Enum")) {
                return ResponseEntity.ok(
                        ApiResponse.error(ErrorCode.INVALID_INPUT, "유효하지 않은 선택값입니다.")
                );
            }
        }

        log.atWarn()
                .setMessage("JSON parsing error")
                .addKeyValue("requestUri", request.getRequestURI())
                .log();

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.INVALID_FORMAT, "요청 형식이 올바르지 않습니다.")
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded() {
        return ResponseEntity.ok(ApiResponse.error(ErrorCode.FILE_SIZE_EXCEEDED));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.atWarn()
                .setMessage("Illegal argument error")
                .addKeyValue("errorMessage", e.getMessage())
                .addKeyValue("requestUri", request.getRequestURI())
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
                .log();

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.OPERATION_NOT_ALLOWED)
        );
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException e) {
        log.atWarn()
                .setMessage("404 Not Found error")
                .addKeyValue("requestUri", e.getRequestURL())
                .addKeyValue("httpMethod", e.getHttpMethod())
                .log();

        return ResponseEntity.status(404).body(
                ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND)
        );
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ApiResponse<Void>> handleJsonProcessingException(JsonProcessingException e) {
        log.error("JSON processing error occurred", e);

        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.SERVER_ERROR, "데이터 처리에 실패했습니다.")
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        if (e.getCause() instanceof IOException) {
            log.error("File IO error occurred (wrapped in RuntimeException)", e.getCause());
            return ResponseEntity.ok(
                    ApiResponse.error(ErrorCode.FILE_UPLOAD_FAILED, "파일 처리에 실패했습니다.")
            );
        }

        log.error("Runtime error occurred", e);
        return ResponseEntity.ok(
                ApiResponse.error(ErrorCode.SERVER_ERROR, "서버 오류가 발생했습니다.")
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
                .setCause(e)
                .log();

        return ResponseEntity.status(500).body(
                ApiResponse.error(ErrorCode.SERVER_ERROR)
        );
    }
}
