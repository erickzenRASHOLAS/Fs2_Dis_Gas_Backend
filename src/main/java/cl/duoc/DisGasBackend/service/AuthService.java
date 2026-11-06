package cl.duoc.DisGasBackend.service;

import cl.duoc.DisGasBackend.dto.AuthResponse;
import cl.duoc.DisGasBackend.dto.LoginRequest;
import cl.duoc.DisGasBackend.dto.RegistroRequest;
import cl.duoc.DisGasBackend.dto.UsuarioResponse;
import cl.duoc.DisGasBackend.exception.ConflictoException;
import cl.duoc.DisGasBackend.model.Estado;
import cl.duoc.DisGasBackend.model.Rol;
import cl.duoc.DisGasBackend.model.Usuario;
import cl.duoc.DisGasBackend.repository.UsuarioRepository;
import cl.duoc.DisGasBackend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String MENSAJE_CREDENCIALES = "Correo o contraseña incorrectos";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** Registro público: siempre crea una cuenta con rol CLIENTE (el rol no lo elige quien se registra). */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest req) {
        String correo = normalizar(req.correo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictoException("El correo electrónico ya se encuentra registrado");
        }
        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setCorreo(correo);
        u.setTelefono(req.telefono().trim());
        u.setDireccion(req.direccion().trim());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRol(Rol.CLIENTE);
        u.setEstado(Estado.ACTIVO);
        return UsuarioResponse.from(usuarioRepository.save(u));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        Usuario u = usuarioRepository.findByCorreo(normalizar(req.correo()))
                .filter(x -> x.getEstado() == Estado.ACTIVO)
                .filter(x -> passwordEncoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException(MENSAJE_CREDENCIALES));
        return emitirTokens(u);
    }

    /** Renovación: valida el refresh token, confirma que el usuario sigue activo y entrega un par nuevo (rotación). */
    @Transactional(readOnly = true)
    public AuthResponse refrescar(String refreshToken) {
        Long id = jwtService.validarRefreshToken(refreshToken);
        Usuario u = usuarioRepository.findById(id)
                .filter(x -> x.getEstado() == Estado.ACTIVO)
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido o expirado"));
        return emitirTokens(u);
    }

    private AuthResponse emitirTokens(Usuario u) {
        return new AuthResponse(
                jwtService.generarAccessToken(u),
                jwtService.generarRefreshToken(u),
                "Bearer",
                jwtService.segundosAccessToken(),
                UsuarioResponse.from(u));
    }

    private String normalizar(String correo) {
        return correo.trim().toLowerCase();
    }
}
