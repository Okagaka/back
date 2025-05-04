package com.okagaka.OkaGaka.domain.zone.entity;


import com.okagaka.OkaGaka.common.utils.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "zone")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Zone extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer familyId;

    @Column(length = 50, nullable = false)
    private String name;

    @Lob
    @Column(nullable = false)
    private String coordinates; // JSON 형식 좌표 (ex. GeoJSON 등)

//    private LocalDateTime createdAt;
//
//    @PrePersist
//    public void prePersist() {
//        this.createdAt = LocalDateTime.now();
//    }
}
