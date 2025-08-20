package com.okagaka.OkaGaka.domain.signup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "회원가입 영역 응답 DTO")
public class ZoneResponse {
    private String name;

    @Column(precision = 10, scale = 6)
    private double latitude;

    @Column(precision = 10, scale = 6)
    private double longitude;
}
