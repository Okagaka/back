//package com.okagaka.OkaGaka.domain.aidecision.service;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Propagation;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//public class AsyncDecisionFacade {
//
//    private final AsyncDecisionService asyncDecisionService;
//
//    // Propagation.REQUIRES_NEW: 항상 새로운 트랜잭션에서 실행되도록 보장
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void processDecisionAsync(Long carRequestId) {
//        asyncDecisionService.processDecision(carRequestId);
//    }
//}
