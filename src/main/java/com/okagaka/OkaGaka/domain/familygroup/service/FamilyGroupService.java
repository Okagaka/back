package com.okagaka.OkaGaka.domain.familygroup.service;

import com.okagaka.OkaGaka.domain.familygroup.repository.FamilyGroupRepository;
import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FamilyGroupService {

    @Autowired
    private FamilyGroupRepository familyGroupRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Long> getUserIdsById(Long groupId) {
//        return familyGroupRepository.findUserIdsById(id);
        return userRepository.findUserIdsByFamilyGroupId(groupId);
    }

    // 특정 사용자가 특정 가족 그룹에 속해 있는지 확인하는 매서드
    public boolean isUserInGroup(Long userId, Long groupId) {
        return userRepository.existsByIdAndFamilyGroup_Id(userId, groupId);
    }
}
