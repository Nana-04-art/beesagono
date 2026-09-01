package com.beesagono.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.beesagono.backend.entity.Role;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.UserRole;
import com.beesagono.backend.entity.id.UserRoleId;
import com.beesagono.backend.enums.RoleName;
import com.beesagono.backend.repository.RoleRepository;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRuoli();
        seedAdmin();
        log.info("DataSeeder Beesagono completato con successo");
    }

    private void seedRuoli() {
        for (RoleName eRole : RoleName.values()) {
            if (roleRepository.findByName(eRole).isEmpty()) {
                Role role = Role.builder()
                        .name(eRole)
                        .build();
                roleRepository.save(role);
                log.info(">>> Ruolo creato: {}", eRole.name());
            }
        }
    }

    private void seedAdmin() {
        String adminEmail = "admin@beesagono.com";

        if (!userRepository.existsByEmail(adminEmail)) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Ruolo ROLE_ADMIN non trovato"));

            User admin = User.builder()
                    .username("admin")
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode("Admin123!"))
                    .build();

            User savedAdmin = userRepository.save(admin);

            // Creates the composite key and the UserRole relationship.
            UserRoleId userRoleId = new UserRoleId(savedAdmin.getId(), adminRole.getId());

            UserRole adminUserRole = UserRole.builder()
                    .id(userRoleId)
                    .user(savedAdmin)
                    .role(adminRole)
                    .build();

            userRoleRepository.save(adminUserRole);

            log.info(">>> Admin di sistema Beesagono creato: {}", adminEmail);
        }
    }
}