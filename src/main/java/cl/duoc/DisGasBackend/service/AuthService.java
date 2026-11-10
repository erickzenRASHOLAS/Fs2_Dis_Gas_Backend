package cl.duoc.DisGasBackend.service;

import cl.duoc.DisGasBackend.dto.Dtos.AuthResponse;
import cl.duoc.DisGasBackend.dto.Dtos.LoginRequest;
import cl.duoc.DisGasBackend.dto.Dtos.RegistroRequest;
import cl.duoc.DisGasBackend.model.Rol;
import cl.duoc.DisGasBackend.model.Usuario;
import cl.duoc.DisGasBackend.repository.UsuarioRepository;
import cl.duoc.DisGasBackend.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Registro, login y renovacion de tokens. */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /** Registro publico: la cuenta SIEMPRE se crea como CLIENTE (el rol no lo elige quien se registra). */
    public Usuario registrar(RegistroRequest req) {
        String correo = req.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya esta registrado");
        }
        Usuario u = new Usuario();
        u.setNombre(req.nombre());
        u.setCorreo(correo);
        u.setTelefono(req.telefono());
        u.setDireccion(req.direccion());
        u.setPassword(passwordEncoder.encode(req.password()));   // se guarda el hash, no la clave
        u.setRol(Rol.CLIENTE);
        return usuarioRepository.save(u);
    }

    public AuthResponse login(LoginRequest req) {
        Usuario u = usuarioRepository.findByCorreo(req.correo().trim().toLowerCase())
                .orElseThrow(() -> credencialesInvalidas());
        // Mismo mensaje si el correo no existe, la clave es mala o la cuenta esta desactivada
        if (!u.isActivo() || !passwordEncoder.matches(req.password(), u.getPassword())) {
            throw credencialesInvalidas();
        }
        return crearRespuesta(u);
    }

    /** Renovacion: con un refresh token valido se entrega un par de tokens nuevo. */
    public AuthResponse refrescar(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtil.leerToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalido o vencido");
        }
        if (!"refresh".equals(claims.get("tipo", String.class))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El token entregado no es un refresh token");
        }
        // Se vuelve a leer el usuario de la BD: si lo desactivaron o borraron, ya no puede renovar
        Usuario u = usuarioRepository.findByCorreo(claims.getSubject())
                .filter(x -> x.isActivo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no disponible"));
        return crearRespuesta(u);
    }

    private AuthResponse crearRespuesta(Usuario u) {
        return new AuthResponse(jwtUtil.crearTokenAcceso(u), jwtUtil.crearTokenRefresh(u), u);
    }

    private ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
    }
}
