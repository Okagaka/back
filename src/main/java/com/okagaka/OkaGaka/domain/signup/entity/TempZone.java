package com.okagaka.OkaGaka.domain.signup.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "temp_zone")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TempZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signup_temp_id")
    private SignupTemp signupTemp;

}
