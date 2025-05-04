package com.okagaka.OkaGaka.domain.user.repository;

import com.okagaka.OkaGaka.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    //
    List<User> findByFamilyGroup_Id(Long familyId);
}
