package com.okagaka.OkaGaka.domain.reservation.service;

import org.springframework.stereotype.Service;

/*
    아직 사용 안 함
 */
@Service
public class NotificationService {

    // 예: DB 저장, FCM 푸시, WebSocket 등 구현 가능
    public void sendNotification(Long userId, String title, String message) {
        // 알림 저장/발송 로직
        System.out.printf("📢 [알림] userId=%d | %s - %s%n", userId, title, message);
    }

}
