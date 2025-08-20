package com.okagaka.OkaGaka.common.external.tmap.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.okagaka.OkaGaka.common.external.tmap.Coordinate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MatrixRouteInfoDTO {
    private int originIndex;
    private int destinationIndex;
    private Coordinate origin;
    private Coordinate destination;
    private int duration; // 초 단위
}
