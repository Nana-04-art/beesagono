package com.beesagono.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:#{null}}")
    private String adminUsername;

    @Value("${app.admin.email:#{null}}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

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
        if (!StringUtils.hasText(adminUsername) || !StringUtils.hasText(adminEmail)
                || !StringUtils.hasText(adminPassword)) {
            log.warn(
                    ">>> Skipping admin creation: APP_ADMIN_USERNAME, APP_ADMIN_EMAIL, or APP_ADMIN_PASSWORD environment variables are not set");
            return;
        }

        if (!userRepository.existsByEmail(adminEmail) && !userRepository.existsByUsername(adminUsername)) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Ruolo ROLE_ADMIN non trovato"));

            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .build();

            User savedAdmin = userRepository.save(admin);

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