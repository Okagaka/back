package com.okagaka.OkaGaka.common.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public enum ErrorCode {

    // 클라이언트 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "사용자 인증이 필요합니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다."),

    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다."),
    GROUP_PASSWORD_INVALID(HttpStatus.FORBIDDEN, "그룹 비밀번호가 일치하지 않습니다."),

    IMAGE_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "이미지 4개 업로드해야 합니다."),
    SIGNUP_TEMP_NOT_FOUND(HttpStatus.NOT_FOUND, "임시 데이터가 존재하지 않습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    CARPOOL_MAXIMUM_EXCEEDED(HttpStatus.BAD_REQUEST, "최대 카풀 인원(4명)을 초과하였습니다."),
    CARPOOL_NOT_POSSIBLE(HttpStatus.CONFLICT, "해당 시간에는 이미 다른 가족 구성원의 예약이 있으며, 카풀 조건이 맞지 않아 예약이 불가능합니다."),
    CARPOOL_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "카풀 가능 인원(3명)을 초과하였습니다."),
//    CARPOOL_NOT_POSSIBLE_WITH_EXISTING("기존 예약 그룹과 카풀이 불가능합니다."),
    CARPOOL_PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "카풀 제안을 찾을 수 없습니다."),
    UNAUTHORIZED_ACTION(HttpStatus.FORBIDDEN, "권한이 없는 동작입니다."),

    TMAP_GUIDE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "길 안내 예측에 실패했습니다."),
    DEPARTURE_TIME_NOT_FOUND(HttpStatus.BAD_REQUEST, "departureTime 값을 찾을 수 없습니다."),
    CARPOOL_DECISION_PENDING(HttpStatus.CONFLICT, "카풀 제안이 아직 처리되지 않았습니다."),
    PROPOSAL_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 제안입니다."),
    DUPLICATE_RESERVATION_TIME(HttpStatus.CONFLICT, "해당 시간대에 이미 자신의 예약이 존재합니다."),

    VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND,"해당 차량을 찾을 수 없습니다."),
    VEHICLE_ALREADY_EXISTS_IN_GROUP(HttpStatus.CONFLICT, "해당 그룹에는 이미 차량이 등록되어 있습니다."),

    CARREQUEST_NOT_FOUND(HttpStatus.NOT_FOUND,"해당 차량 요청 기록을 찾을 수 없습니다."),

    DECISION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 AI 결과를 찾을 수 없습니다."),




//    MEMORY_CREATE_FAILED(HttpStatus.BAD_REQUEST, "게시글 생성에 실패했습니다."),
//    MEMORY_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getStatus() {
        return httpStatus.value();
    }
}

