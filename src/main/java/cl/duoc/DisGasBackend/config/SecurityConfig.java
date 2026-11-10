package cl.duoc.DisGasBackend.config;

import cl.duoc.DisGasBackend.security.JwtAuthFilter;
import cl.duoc.DisGasBackend.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Aqui se define QUIEN puede entrar a CADA ruta (autorizacion por roles).
 * Spring evalua las reglas en orden y usa la primera que coincide.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /** Las contraseñas se guardan con BCrypt (hash de una sola via). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())                 // no usamos cookies, por eso no hace falta CSRF
                .cors(Customizer.withDefaults())              // usa la configuracion de CorsConfig
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // sin sesion en el servidor
                .authorizeHttpRequests(auth -> auth
                        // --- Rutas publicas ---
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()

                        // --- Productos y categorias: ver la tienda es publico, modificar es solo del ADMIN ---
                        .requestMatchers(HttpMethod.GET, "/v1/productos/todos").hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                        .requestMatchers(HttpMethod.GET, "/v1/productos/**", "/v1/categorias/**").permitAll()
                        .requestMatchers("/v1/productos/**", "/v1/categorias/**").hasRole("ADMINISTRADOR")

                        // --- Usuarios: solo ADMIN (el perfil propio esta en /v1/perfil) ---
                        .requestMatchers("/v1/usuarios/**").hasRole("ADMINISTRADOR")

                        // --- Boletas ---
                        .requestMatchers(HttpMethod.GET, "/v1/boletas/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR", "CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/v1/boletas").hasAnyRole("ADMINISTRADOR", "CLIENTE")
                        .requestMatchers("/v1/boletas/*/cancelar").hasAnyRole("ADMINISTRADOR", "CLIENTE")
                        .requestMatchers("/v1/boletas/**").hasRole("ADMINISTRADOR")

                        // --- Todoo lo demas (ej: /v1/perfil): cualquier usuario con sesion ---
                        .anyRequest().authenticated())
                // Respuestas cuando no se puede entrar
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> responder(res, 401, "Debes iniciar sesion (token ausente, invalido o vencido)"))
                        .accessDeniedHandler((req, res, ex) -> responder(res, 403, "No tienes permisos para esta accion")))
                // Nuestro filtro JWT corre antes del filtro de login por defecto de Spring
                .addFilterBefore(new JwtAuthFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void responder(HttpServletResponse res, int status, String mensaje) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"mensaje\":\"" + mensaje + "\"}");
    }
}
