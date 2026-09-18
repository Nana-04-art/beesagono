package com.beesagono.backend.service;

import com.beesagono.backend.dto.auth.CreateAdminRequest;
import com.beesagono.backend.dto.auth.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    UserResponse createAdmin(CreateAdminRequest request);

    Page<UserResponse> getUsers(String search, Pageable pageable);
}