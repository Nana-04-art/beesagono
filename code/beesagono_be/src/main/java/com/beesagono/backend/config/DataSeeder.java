package com.beesagono.backend.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.beesagono.backend.entity.ErrorType;
import com.beesagono.backend.entity.Role;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.UserRole;
import com.beesagono.backend.entity.id.UserRoleId;
import com.beesagono.backend.enums.ErrorTypeCode;
import com.beesagono.backend.enums.RoleName;
import com.beesagono.backend.repository.ErrorTypeRepository;
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
    private final ErrorTypeRepository errorTypeRepository;
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
        seedErrorTypes();
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

    private void seedErrorTypes() {
        Map<ErrorTypeCode, String> errorTypes = Map.of(
                ErrorTypeCode.TOO_SHORT, "La parola deve contenere almeno 4 lettere.",
                ErrorTypeCode.MISSING_CENTER, "La parola non contiene la lettera centrale obbligatoria.",
                ErrorTypeCode.INVALID_LETTERS, "La parola contiene lettere non presenti nell'alveare.",
                ErrorTypeCode.ALREADY_FOUND, "Hai già trovato questa parola in questa sessione.",
                ErrorTypeCode.NOT_IN_DICTIONARY, "Parola non è presente nel dizionario ufficiale.",
                ErrorTypeCode.NOT_IN_PUZZLE, "La parola non fa parte delle soluzioni del puzzle di oggi.");

        errorTypes.forEach((code, description) -> {
            if (!errorTypeRepository.existsById(code)) {
                ErrorType errorType = ErrorType.builder()
                        .code(code)
                        .description(description)
                        .build();
                errorTypeRepository.save(errorType);
                log.info(">>> ErrorType creato: {}", code.name());
            }
        });
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

            Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Ruolo ROLE_USER non trovato"));

            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .build();

            User savedAdmin = userRepository.save(admin);

            // Assegnazione ruolo ADMIN
            UserRoleId adminRoleId = new UserRoleId(savedAdmin.getId(), adminRole.getId());
            UserRole adminUserRole = UserRole.builder()
                    .id(adminRoleId)
                    .user(savedAdmin)
                    .role(adminRole)
                    .build();
            userRoleRepository.save(adminUserRole);

            // Assegnazione ruolo USER
            UserRoleId userRoleId = new UserRoleId(savedAdmin.getId(), userRole.getId());
            UserRole userUserRole = UserRole.builder()
                    .id(userRoleId)
                    .user(savedAdmin)
                    .role(userRole)
                    .build();
            userRoleRepository.save(userUserRole);

            log.info(">>> Admin di sistema Beesagono creato (ROLE_USER + ROLE_ADMIN): {}", adminEmail);
        }
    }
}