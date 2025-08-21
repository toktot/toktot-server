package com.toktot.external.kakao.config;

import com.toktot.external.kakao.KakaoApiConstants;
import com.toktot.external.kakao.exception.KakaoApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class KakaoApiErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        HttpStatusCode statusCode = response.getStatusCode();
        return statusCode.isError();
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatusCode statusCode = response.getStatusCode();
        String errorMessage = extractErrorMessage(response);
        int statusCodeValue = statusCode.value();

        log.error(KakaoApiConstants.LOG_API_CALL_FAILURE, "KAKAO_MAP", errorMessage);

        HttpStatus httpStatus = HttpStatus.valueOf(statusCodeValue);

        switch (httpStatus) {
            case BAD_REQUEST:
                throw new KakaoApiException(KakaoApiConstants.ERROR_INVALID_API_KEY,
                        "잘못된 요청 파라미터입니다: " + errorMessage);
            case UNAUTHORIZED:
                throw new KakaoApiException(KakaoApiConstants.ERROR_INVALID_API_KEY,
                        "유효하지 않은 API 키입니다: " + errorMessage);
            case TOO_MANY_REQUESTS:
                throw new KakaoApiException(KakaoApiConstants.ERROR_QUOTA_EXCEEDED,
                        "API 호출 한도를 초과했습니다: " + errorMessage);
            case INTERNAL_SERVER_ERROR:
                throw new KakaoApiException(KakaoApiConstants.ERROR_NETWORK,
                        "카카오 서버 내부 오류입니다: " + errorMessage);
            case BAD_GATEWAY:
                throw new KakaoApiException(KakaoApiConstants.ERROR_NETWORK,
                        "카카오 서버 게이트웨이 오류입니다: " + errorMessage);
            default:
                if (statusCodeValue >= 500) {
                    throw new KakaoApiException(KakaoApiConstants.ERROR_NETWORK,
                            "카카오 서버 오류입니다 (" + statusCodeValue + "): " + errorMessage);
                } else {
                    throw new KakaoApiException(KakaoApiConstants.ERROR_NETWORK,
                            "알 수 없는 오류가 발생했습니다 (" + statusCodeValue + "): " + errorMessage);
                }
        }
    }

    private String extractErrorMessage(ClientHttpResponse response) {
        try {
            byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());
            String errorMessage = new String(responseBody, StandardCharsets.UTF_8);

            if (errorMessage.trim().isEmpty()) {
                return "응답 메시지가 없습니다";
            }

            if (errorMessage.length() > 200) {
                return errorMessage.substring(0, 200) + "...";
            }

            return errorMessage;

        } catch (IOException e) {
            log.warn("에러 응답 본문 읽기 실패: {}", e.getMessage());
            return "응답 메시지를 읽을 수 없습니다";
        } catch (Exception e) {
            log.warn("에러 메시지 처리 중 예상치 못한 오류: {}", e.getMessage());
            return "에러 메시지 처리 실패";
        }
    }
}
