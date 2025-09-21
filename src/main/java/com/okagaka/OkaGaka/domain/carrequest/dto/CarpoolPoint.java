package com.okagaka.OkaGaka.domain.carrequest.dto;

import com.okagaka.OkaGaka.domain.carrequest.entity.CarRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString(of = {"carRequest", "isPickup", "lat", "lon"})
@RequiredArgsConstructor
public class CarpoolPoint {
    private final CarRequest carRequest;
    private final boolean isPickup;
    private final double lat;
    private final double lon;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CarpoolPoint that = (CarpoolPoint) o;
        return isPickup == that.isPickup && Objects.equals(carRequest.getId(), that.carRequest.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(carRequest.getId(), isPickup);
    }
}


