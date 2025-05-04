package com.okagaka.OkaGaka.domain.zone.repository;

import com.okagaka.OkaGaka.domain.zone.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    List<Zone> findByFamilyId(Integer familyId);
}
