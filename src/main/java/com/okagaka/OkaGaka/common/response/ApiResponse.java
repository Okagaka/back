package com.okagaka.OkaGaka.common.response;

import com.okagaka.OkaGaka.common.exception.ErrorCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;

    public ApiResponse(SuccessCode code, T data) {
        this.status = code.getStatus();
        this.message = code.getMessage();
        this.data = data;
    }

    public ApiResponse(ErrorCode code, T data) {
        this.status = code.getStatus();
        this.message = code.getMessage();
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SuccessCode.SUCCESS, data);
    }

    public static <T> ApiResponse<T> error(ErrorCode code) {
        return new ApiResponse<>(code, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode code, T data) {
        return new ApiResponse<>(code, data);
    }
}

