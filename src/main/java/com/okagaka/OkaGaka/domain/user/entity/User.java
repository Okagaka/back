package com.okagaka.OkaGaka.domain.user.entity;

import com.okagaka.OkaGaka.common.utils.BaseTimeEntity;
import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;
import com.okagaka.OkaGaka.domain.zone.entity.Zone;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.ArrayList;
//import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    private FamilyGroup familyGroup;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Zone> zones = new ArrayList<>();
//    @OneToMany(mappedBy = "user")
//    private List<Zone> zones = new ArrayList<>();

    private String name;

    @Column(name = "phone_number", length = 15, nullable = false) // 다른 것도 nullable 설정하기
    private String phoneNumber;

    private String location; // 이거에 제약 줄 수 있나?

    private String condition;

    public void updateCondition(String condition) {
        this.condition = condition;
    }

}
