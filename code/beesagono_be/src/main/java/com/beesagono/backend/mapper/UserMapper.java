package com.beesagono.backend.mapper;

import com.beesagono.backend.dto.auth.RegisterRequest;
import com.beesagono.backend.dto.auth.UserResponse;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "userRoles", qualifiedByName = "mapUserRolesToNames")
    UserResponse toUserResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "registeredAt", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    @Mapping(target = "addedWords", ignore = true)
    @Mapping(target = "gameSessions", ignore = true)
    @Mapping(target = "playerSeasons", ignore = true)
    @Mapping(target = "rankHistogramEntries", ignore = true)
    @Mapping(target = "playerStats", ignore = true)
    User toUser(RegisterRequest request);

    @Named("mapUserRolesToNames")
    default Set<String> mapUserRolesToNames(List<UserRole> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return Collections.emptySet();
        }
        return userRoles.stream()
                .filter(ur -> ur.getRole() != null && ur.getRole().getName() != null)
                .map(ur -> ur.getRole().getName().name())
                .collect(Collectors.toSet());
    }
}