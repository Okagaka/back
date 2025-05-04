package com.okagaka.OkaGaka.domain.familygroup.entity;

import com.okagaka.OkaGaka.domain.vehicle.entity.Vehicle;
import com.okagaka.OkaGaka.domain.user.entity.User;
//import com.okagaka.OkaGaka.domain.zone.entity.Zone;
import jakarta.persistence.*;
import java.util.List;
import java.util.ArrayList;
import lombok.*;
import com.okagaka.OkaGaka.common.utils.BaseTimeEntity;

@Entity
@Table(name = "family_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FamilyGroup extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 차량과 연관 관계 (one-to-one)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Builder.Default
    @OneToMany(mappedBy = "familyGroup")
    private List<User> users = new ArrayList<>();

    @Column(nullable = false, length = 50)
    private String name; // 가족 그룹명


}
