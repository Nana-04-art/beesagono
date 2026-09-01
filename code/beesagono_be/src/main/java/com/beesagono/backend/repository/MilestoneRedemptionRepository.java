package com.beesagono.backend.repository;

import com.beesagono.backend.entity.MilestoneRedemption;
import com.beesagono.backend.entity.id.MilestoneRedemptionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MilestoneRedemptionRepository extends JpaRepository<MilestoneRedemption, MilestoneRedemptionId> {
    // Navigation of the id.userId and id.year fields of the composite key
    List<MilestoneRedemption> findByIdUserIdAndIdYear(String userId, Integer year);
}