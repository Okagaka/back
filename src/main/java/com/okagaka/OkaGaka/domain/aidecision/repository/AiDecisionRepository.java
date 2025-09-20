package com.okagaka.OkaGaka.domain.aidecision.repository;

import com.okagaka.OkaGaka.domain.aidecision.entity.AiDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AiDecisionRepository extends JpaRepository<AiDecision, Long> {
    Optional<AiDecision> findByCarRequestId(Long carRequestId);
}
