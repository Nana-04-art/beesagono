package com.beesagono.backend.specification;

import com.beesagono.backend.dto.dictionary.DictionaryFilterRequest;
import com.beesagono.backend.entity.DictionaryWord;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class DictionaryWordSpecification {

    public static Specification<DictionaryWord> buildSpecification(DictionaryFilterRequest filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filtro.getSearch() != null && !filtro.getSearch().isBlank()) {
                String searchPattern = "%" + filtro.getSearch().trim().toUpperCase() + "%";
                predicates.add(cb.like(root.get("word"), searchPattern));
            }

            if (filtro.getWordLength() != null) {
                predicates.add(cb.equal(root.get("wordLength"), filtro.getWordLength()));
            }

            if (filtro.getIsCandidatePangram() != null) {
                predicates.add(cb.equal(root.get("isCandidatePangram"), filtro.getIsCandidatePangram()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}