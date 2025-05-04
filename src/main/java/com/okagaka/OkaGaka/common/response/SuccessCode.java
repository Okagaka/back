package com.okagaka.OkaGaka.common.response;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public enum SuccessCode {
    // 성공 코드
    SUCCESS(HttpStatus.OK, "요청이 성공적으로 처리되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    SuccessCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getStatus() {
        return httpStatus.value();
    }

}
