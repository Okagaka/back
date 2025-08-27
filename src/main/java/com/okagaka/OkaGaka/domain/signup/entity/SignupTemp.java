package com.okagaka.OkaGaka.domain.signup.entity;

import jakarta.persistence.*;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.ArrayList;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupTemp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phoneNumber;

    @ElementCollection
    private List<String> faceImages;

    // 임시 영역 (Zone) 정보 저장
//    @OneToMany(mappedBy = "signupTemp", cascade = CascadeType.ALL, orphanRemoval = true)
//    @Builder.Default
//    private List<TempZone> tempZones = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "signupTemp", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TempZone> tempZones = new ArrayList<>();


    private String vehicleModel;
    private String address;

    private long familyId;


}
