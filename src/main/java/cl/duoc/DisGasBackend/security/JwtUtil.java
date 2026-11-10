package cl.duoc.DisGasBackend.security;

import cl.duoc.DisGasBackend.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Crea y lee tokens JWT.
 * Un JWT lleva 3 partes: cabecera.datos.firma. La firma se hace con una clave secreta (jwt.secret),
 * asi nadie puede modificar el token sin que el servidor lo note.
 *
 * Hay dos tipos de token:
 *  - "access":  dura poco (60 min). Se envia en cada peticion: Authorization: Bearer <token>
 *  - "refresh": dura mas (7 dias). Solo sirve para pedir un token nuevo en /v1/auth/refresh
 */
@Component
public class JwtUtil {

    private final SecretKey clave;
    private final long accesoMs;
    private final long refreshMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiracion-minutos}") long minutos,
                   @Value("${jwt.refresh-dias}") long dias) {
        // Si la clave tiene menos de 32 caracteres, esta linea lanza error y la app no arranca
        this.clave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accesoMs = minutos * 60 * 1000;
        this.refreshMs = dias * 24 * 60 * 60 * 1000;
    }

    public String crearTokenAcceso(Usuario usuario) {
        return construir(usuario, "access", accesoMs);
    }

    public String crearTokenRefresh(Usuario usuario) {
        return construir(usuario, "refresh", refreshMs);
    }

    private String construir(Usuario usuario, String tipo, long duracionMs) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(usuario.getCorreo())            // a quien pertenece el token
                .claim("rol", usuario.getRol().name())   // su rol (ADMINISTRADOR, VENDEDOR, CLIENTE)
                .claim("tipo", tipo)                     // access o refresh
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + duracionMs))
                .signWith(clave)                         // firma con la clave secreta
                .compact();
    }

    /** Verifica la firma y la fecha de vencimiento. Si el token no es valido lanza JwtException. */
    public Claims leerToken(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
