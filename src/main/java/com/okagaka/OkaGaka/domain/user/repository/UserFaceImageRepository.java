package com.okagaka.OkaGaka.domain.user.repository;

import com.okagaka.OkaGaka.domain.user.entity.UserFaceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserFaceImageRepository extends JpaRepository<UserFaceImage, Long> {
    // 특정 사용자 ID로 이미지 목록 조회
    List<UserFaceImage> findByUserId(Long userId);
}
