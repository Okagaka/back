package com.okagaka.OkaGaka.domain.signup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.okagaka.OkaGaka.domain.signup.entity.SignupTemp;

public interface SignupTempRepository extends JpaRepository<SignupTemp, Long>{
    boolean existsByPhoneNumber(String phoneNumber);

}
