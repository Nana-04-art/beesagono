package com.beesagono.backend.entity.id;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class RankHistogramId implements Serializable {

    private String userId;
    private String rankLabel;
}