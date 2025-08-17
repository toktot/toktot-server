package com.toktot.external.kakao.config;

import com.toktot.external.kakao.KakaoApiConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

@Slf4j
public class KakaoApiErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = (HttpStatus) response.getStatusCode();
        String errorMessage = getErrorMessage(response);

        log.error(KakaoApiConstants.LOG_API_CALL_FAILURE, "KAKAO_API", errorMessage);

        switch (statusCode) {
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
            case BAD_GATEWAY:
            case SERVICE_UNAVAILABLE:
                throw new KakaoApiException(KakaoApiConstants.ERROR_NETWORK,
                        "카카오 서버 오류입니다: " + errorMessage);
            default:
                throw new KakaoApiException(KakaoApiConstants.ERROR_NETWORK,
                        "알 수 없는 오류가 발생했습니다: " + errorMessage);
        }
    }

    private String getErrorMessage(ClientHttpResponse response) throws IOException {
        try {
            return new String(response.getBody().readAllBytes());
        } catch (Exception e) {
            return "응답 메시지를 읽을 수 없습니다";
        }
    }

    public static class KakaoApiException extends RuntimeException {
        private final String errorCode;

        public KakaoApiException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
