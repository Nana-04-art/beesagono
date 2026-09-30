package com.beesagono.backend.entity.id;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Composite primary key for mapping player distribution counts across distinct
 * rank tier labels.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class RankHistogramId implements Serializable {

    @Column(name = "user_id")
    private String userId;

    @Column(name = "rank_label")
    private String rankLabel;
}