package com.okagaka.OkaGaka.domain.user.repository;

import com.okagaka.OkaGaka.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByFamilyGroup_Id(Long familyId);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<User> findByNameAndPhoneNumber(String name, String phoneNumber);

    @Query("SELECT u.id FROM User u WHERE u.familyGroup.id = :familyId")
    List<Long> findUserIdsByFamilyGroupId(@Param("familyId") Long familyId);

    @Query("SELECT u FROM User u JOIN FETCH u.familyGroup WHERE u.id = :id")
    Optional<User> findByIdWithFamilyGroup(@Param("id") Long id);

    boolean existsByIdAndFamilyGroup_Id(Long userId, Long familyId);

    Optional<User> findByName(String name);

    List<User> findAllByFamilyGroupId(Long familyGroupId);
}
