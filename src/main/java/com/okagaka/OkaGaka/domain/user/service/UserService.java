package com.okagaka.OkaGaka.domain.user.service;

import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.common.security.JwtTokenProvider;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // 1. 반환 타입을 위한 record 정의 (UserService 내부에 선언하거나 별도 파일로 생성 가능)
    public record LoginResult(String token, Long userId, Long groupId) {}

    // 2. login 메소드 수정
    public LoginResult login(String name, String phoneNumber) {
        User user = userRepository.findByNameAndPhoneNumber(name, phoneNumber)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 토큰 생성
        String token = jwtTokenProvider.createToken(user.getId(), user.getName());

        // userId 추출
        Long userId = user.getId();

        // groupId 추출 (그룹이 없을 경우 null 처리)
        Long groupId = (user.getFamilyGroup() != null) ? user.getFamilyGroup().getId() : null;

        // 모든 정보를 담은 LoginResult 객체 반환
        return new LoginResult(token, userId, groupId);
    }

//    public String login(String name, String phoneNumber) {
//        System.out.println(phoneNumber);
//        User user = userRepository.findByNameAndPhoneNumber(name, phoneNumber)
//                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
//
//        return jwtTokenProvider.createToken(user.getId(), user.getName());
//    }
}

