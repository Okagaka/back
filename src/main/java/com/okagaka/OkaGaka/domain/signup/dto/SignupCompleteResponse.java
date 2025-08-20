package com.okagaka.OkaGaka.domain.signup.dto;

import jakarta.persistence.Lob;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupCompleteResponse {

    private Long userId;
    private String userName;
    private String phoneNumber;
    private List<String> imageUrls;
    private Long familyId;

    private Long zoneId;
    private String zoneName;
    private Double zoneLatitude;
    private Double zoneLongitude;
}
