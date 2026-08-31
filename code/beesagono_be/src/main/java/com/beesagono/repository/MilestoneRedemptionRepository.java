package com.beesagono.repository;

import com.beesagono.entity.MilestoneRedemption;
import com.beesagono.entity.MilestoneRedemption.MilestoneRedemptionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MilestoneRedemptionRepository extends JpaRepository<MilestoneRedemption, MilestoneRedemptionId> {
    List<MilestoneRedemption> findByUserIdAndYear(String userId, Integer year);
}