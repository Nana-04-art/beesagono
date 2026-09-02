package com.beesagono.backend.repository;

import com.beesagono.backend.entity.ErrorType;
import com.beesagono.backend.enums.ErrorTypeCode;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorTypeRepository extends JpaRepository<ErrorType, ErrorTypeCode> {
}