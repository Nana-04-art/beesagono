package com.beesagono.backend.controller;

import com.beesagono.backend.dto.auth.CreateAdminRequest;
import com.beesagono.backend.dto.auth.UserResponse;
import com.beesagono.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Users Controller", description = "Administrative endpoints for user management and admin creation")
public class AdminUserController {

    private final AdminService adminService;

    @Operation(summary = "Get paginated user list", description = "Retrieves a paginated list of registered users with an optional search filter for username or email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User list retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized user", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error while retrieving users", content = @Content)
    })
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getUsers(
            @Parameter(description = "Optional search term to filter users by username or email", example = "john") @RequestParam(required = false) String search,
            @Parameter(description = "Pagination and sorting parameters (page, size, sort)") Pageable pageable) {
        return ResponseEntity.ok(adminService.getUsers(search, pageable));
    }

    @Operation(summary = "Create a new administrator user", description = "Registers a new user account with administrative privileges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Administrator created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or validation failure", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized user", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required", content = @Content),
            @ApiResponse(responseCode = "409", description = "Username or email is already in use", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during admin creation", content = @Content)
    })
    @PostMapping
    public ResponseEntity<UserResponse> createAdmin(
            @Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createAdmin(request));
    }
}