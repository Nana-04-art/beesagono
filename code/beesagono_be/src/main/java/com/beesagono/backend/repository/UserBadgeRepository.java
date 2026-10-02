package com.beesagono.backend.repository;

import com.beesagono.backend.entity.UserBadge;
import com.beesagono.backend.entity.id.UserBadgeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, UserBadgeId> {

    List<UserBadge> findByIdUserId(String userId);

    boolean existsByIdUserIdAndIdBadgeCode(String userId, String badgeCode);
}