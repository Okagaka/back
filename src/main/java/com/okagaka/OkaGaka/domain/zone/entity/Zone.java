package com.okagaka.OkaGaka.domain.zone.entity;


import com.okagaka.OkaGaka.common.utils.BaseTimeEntity;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;
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

//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "family_id", nullable = false)
//    private FamilyGroup familyGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 50, nullable = false)
    private String name;

//    @Lob
//    @Column(nullable = false)
//    private String coordinates; // JSON 형식 좌표 (ex. GeoJSON 등)

    @Column
    private double latitude;

    @Column
    private double longitude;

//    private Boolean isHomeAddress;

//    private LocalDateTime createdAt;
//
//    @PrePersist
//    public void prePersist() {
//        this.createdAt = LocalDateTime.now();
//    }
}
