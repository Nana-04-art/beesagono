package com.beesagono.repository;

import com.beesagono.entity.RankHistogram;
import com.beesagono.entity.RankHistogram.RankHistogramId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RankHistogramRepository extends JpaRepository<RankHistogram, RankHistogramId> {
    List<RankHistogram> findByUserId(String userId);
}