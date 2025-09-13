package com.okagaka.OkaGaka.domain.signup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FamilyCreateResponse {
    private Long familyId;
    private String familyName;
//    private String vehicleModel;

    @Column(precision = 10, scale = 6)
    private double homeLatitude;

    @Column(precision = 10, scale = 6)
    private double homeLongitude;
}
