package cl.duoc.DisGasBackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro que se ejecuta en CADA peticion, antes de llegar al controlador:
 *  1. Lee el header  Authorization: Bearer <token>
 *  2. Valida el token (firma y vencimiento)
 *  3. Si es valido, le dice a Spring Security quien es el usuario y que rol tiene
 * Si no hay token o es invalido, la peticion sigue como "anonima" y Spring Security
 * la rechaza (401) si el endpoint exige estar autenticado.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.leerToken(header.substring(7));
                // Solo los tokens "access" sirven para autenticarse (un refresh token no)
                if ("access".equals(claims.get("tipo", String.class))) {
                    String rol = claims.get("rol", String.class);
                    UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(
                            claims.getSubject(), null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            } catch (JwtException | IllegalArgumentException e) {
                // Token vencido, alterado o mal formado: se ignora y la peticion queda sin autenticar
            }
        }
        chain.doFilter(request, response);
    }
}
