package io.innovation.ekoc.config;

import io.innovation.ekoc.users.domain.Role;
import io.innovation.ekoc.users.domain.RoleType;
import io.innovation.ekoc.users.domain.User;
import io.innovation.ekoc.users.repository.RoleRepository;
import io.innovation.ekoc.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Seeds a default admin user for local development only.
 * Never runs in non-local environments.
 * Credentials: admin / admin123
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername("admin")) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleType.ADMIN)
                .orElseGet(() -> {
                    log.warn("ADMIN role not found; skipping dev admin seed");
                    return null;
                });
        if (adminRole == null) return;

        User admin = User.builder()
                .username("admin")
                .email("admin@ekoc.local")
                .password(passwordEncoder.encode("admin123"))
                .fullName("System Administrator")
                .roles(Set.of(adminRole))
                .build();
        userRepository.save(admin);
        log.info("[DEV] Seeded default admin user (admin/admin123) — local profile only");
    }
}
