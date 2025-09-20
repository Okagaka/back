package com.okagaka.OkaGaka.domain.carrequest.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CarRequestDto {

    // 요청자 현재 위치
    private String requesterCityDo;
    private String requesterGuGun;
    private String requesterDong;
    private String requesterBunji;

    // 도착지
    private String destinationCityDo;
    private String destinationGuGun;
    private String destinationDong;
    private String destinationBunji;

}
