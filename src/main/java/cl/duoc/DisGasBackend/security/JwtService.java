package cl.duoc.DisGasBackend.security;

import cl.duoc.DisGasBackend.model.Usuario;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

/**
 * Genera y valida tokens JWT firmados con HS256.
 * - access token: corto (30 min), lleva el rol; es el que autoriza las peticiones.
 * - refresh token: largo (7 días), solo sirve para pedir un nuevo par de tokens.
 * Ambos llevan el claim "token_type" para que uno no pueda usarse como el otro.
 */
@Service
public class JwtService {

    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";

    private final JwtEncoder encoder;
    private final JwtProperties props;
    private final JwtDecoder refreshDecoder;

    public JwtService(JwtEncoder encoder, SecretKey jwtSecretKey, JwtProperties props) {
        this.encoder = encoder;
        this.props = props;
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.issuer()));
        this.refreshDecoder = decoder;
    }

    public long segundosAccessToken() {
        return Duration.ofMinutes(props.accessExpirationMinutes()).toSeconds();
    }

    public String generarAccessToken(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(Duration.ofMinutes(props.accessExpirationMinutes())))
                .subject(String.valueOf(usuario.getId()))
                .id(UUID.randomUUID().toString())
                .claim("correo", usuario.getCorreo())
                .claim("nombre", usuario.getNombre())
                .claim("roles", List.of(usuario.getRol().name()))
                .claim("token_type", TIPO_ACCESS)
                .build();
        return firmar(claims);
    }

    public String generarRefreshToken(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(Duration.ofDays(props.refreshExpirationDays())))
                .subject(String.valueOf(usuario.getId()))
                .id(UUID.randomUUID().toString())
                .claim("token_type", TIPO_REFRESH)
                .build();
        return firmar(claims);
    }

    /** Valida firma, emisor, expiración y tipo del refresh token; devuelve el id del usuario. */
    public Long validarRefreshToken(String token) {
        try {
            Jwt jwt = refreshDecoder.decode(token);
            if (!TIPO_REFRESH.equals(jwt.getClaimAsString("token_type"))) {
                throw new BadCredentialsException("El token entregado no es un refresh token");
            }
            return Long.valueOf(jwt.getSubject());
        } catch (JwtException | NumberFormatException e) {
            throw new BadCredentialsException("Refresh token inválido o expirado");
        }
    }

    private String firmar(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
