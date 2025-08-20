package com.okagaka.OkaGaka.domain.familygroup.repository;

import com.okagaka.OkaGaka.domain.carrequest.entity.CarRequest;
import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FamilyGroupRepository extends JpaRepository<FamilyGroup, Long> {
    boolean existsByName(String name);
    Optional<FamilyGroup> findByName(String name);
    List<Long> findUserIdsById(Long id);
}
