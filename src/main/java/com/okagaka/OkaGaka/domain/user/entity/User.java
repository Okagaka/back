package com.okagaka.OkaGaka.domain.user.entity;

import com.okagaka.OkaGaka.common.utils.BaseTimeEntity;
import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;
import jakarta.persistence.*;
import lombok.*;
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

    private String name;

    @Column(name = "phone_number", length = 15, nullable = false) // 다른 것도 nullable 설정하기
    private String phoneNumber;

    private String location; // 이거에 제약 줄 수 있나?

    private String condition;

}
