package com.okagaka.OkaGaka.domain.reservation.enums;

/*
    PENDING:    다른 예약자 응답 대기 중(카풀 제안된 상태)
    CONFIRMED:  단독 혹은 양쪽 모두 승인한 예약
    REJECTED:   기존 예약자 혹은 본인이 거절
    CANCELLED:  예약자가 자발적으로 취소
    COMPLETED:  일정 완료
    CONFLICT: 시간 충돌로 자동 거절됨
*/
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CARPOOL,
    REJECTED,
    CANCELLED,
    COMPLETED,
    COMPLICT
}
