package com.okagaka.OkaGaka.domain.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;
import com.okagaka.OkaGaka.common.utils.BaseTimeEntity;
import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;

@Entity
@Table(name = "vehicle")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Vehicle extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private VehicleStatus status;

    @Column(nullable = false)
    private String location;

    @Builder.Default
    @Column(name = "battery_level", nullable = false)
    private Integer batteryLevel = 100;  // 기본값 설정 가능

    @Builder.Default
    @Column(nullable = false)
    private Float speed = 0f;  // 기본값 설정 가능

    @Builder.Default
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    // ✅ FamilyGroup과의 양방향 1:1 매핑
    @OneToOne(mappedBy = "vehicle", fetch = FetchType.LAZY)
    private FamilyGroup familyGroup;

    public enum VehicleStatus {
        Idle,
        Moving,
        Charging,
        Unavailable
    }

}
