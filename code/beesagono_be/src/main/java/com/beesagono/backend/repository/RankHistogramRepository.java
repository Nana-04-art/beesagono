package com.beesagono.backend.repository;

import com.beesagono.backend.entity.RankHistogram;
import com.beesagono.backend.entity.id.RankHistogramId;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RankHistogramRepository extends JpaRepository<RankHistogram, RankHistogramId> {
    List<RankHistogram> findByIdUserId(String userId);
}