package cl.duoc.DisGasBackend.security;

import cl.duoc.DisGasBackend.model.Rol;
import java.util.Optional;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** Actor autenticado; lanza 401 si no hay sesión. */
    public static Actor actorActual() {
        return actorOpcional().orElseThrow(() -> new AuthenticationCredentialsNotFoundException("No autenticado"));
    }

    /** Actor autenticado o vacío si la petición es anónima (endpoints públicos). */
    public static Optional<Actor> actorOpcional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth) || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Long id;
        try {
            id = Long.valueOf(jwtAuth.getToken().getSubject());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String nombre = authority.getAuthority();
            if (nombre != null && nombre.startsWith("ROLE_")) {
                try {
                    return Optional.of(new Actor(id, Rol.valueOf(nombre.substring(5))));
                } catch (IllegalArgumentException ignorado) {
                    // rol desconocido: se ignora
                }
            }
        }
        return Optional.empty();
    }
}
