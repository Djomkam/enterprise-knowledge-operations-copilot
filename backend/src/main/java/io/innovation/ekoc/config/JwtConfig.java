package io.innovation.ekoc.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "security.jwt")
public class JwtConfig {

    private static final String INSECURE_DEFAULT = "change-this-secret-in-production-min-256-bits";

    private String secret;
    private Long expiration;
    private Long refreshExpiration;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @PostConstruct
    void validateSecret() {
        if (INSECURE_DEFAULT.equals(secret) && !"local".equals(activeProfile) && !"test".equals(activeProfile)) {
            throw new IllegalStateException(
                "JWT_SECRET must be set to a cryptographically random value of at least 256 bits " +
                "in non-local environments. Current profile: " + activeProfile);
        }
    }
}
