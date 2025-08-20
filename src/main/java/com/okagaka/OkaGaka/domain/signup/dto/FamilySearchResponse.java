package com.okagaka.OkaGaka.domain.signup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FamilySearchResponse {
    private Long familyId;
    private boolean exists;
    private String familyName;
}