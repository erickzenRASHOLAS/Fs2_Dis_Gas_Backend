package cl.duoc.DisGasBackend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessExpirationMinutes,
        long refreshExpirationDays) {
}
