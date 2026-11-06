package cl.duoc.DisGasBackend.controller;

import cl.duoc.DisGasBackend.dto.AuthResponse;
import cl.duoc.DisGasBackend.dto.LoginRequest;
import cl.duoc.DisGasBackend.dto.RefreshRequest;
import cl.duoc.DisGasBackend.dto.RegistroRequest;
import cl.duoc.DisGasBackend.dto.UsuarioResponse;
import cl.duoc.DisGasBackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro, inicio de sesión y renovación de tokens JWT (endpoints públicos)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un cliente", description = "Crea una cuenta con rol CLIENTE. Responde 409 si el correo ya existe.")
    public UsuarioResponse registro(@Valid @RequestBody RegistroRequest req) {
        return authService.registrar(req);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Devuelve un access token (30 min) y un refresh token (7 días). Responde 401 si las credenciales son incorrectas.")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar tokens", description = "Recibe un refresh token válido y devuelve un nuevo par de tokens.")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refrescar(req.refreshToken());
    }
}
