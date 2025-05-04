package com.okagaka.OkaGaka.domain.familygroup.repository;

import com.okagaka.OkaGaka.domain.carrequest.entity.CarRequest;
import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FamilyGroupRepository extends JpaRepository<CarRequest, Long> {
}
