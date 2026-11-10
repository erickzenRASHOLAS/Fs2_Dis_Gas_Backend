package cl.duoc.DisGasBackend.controller;

import cl.duoc.DisGasBackend.dto.Dtos.AuthResponse;
import cl.duoc.DisGasBackend.dto.Dtos.LoginRequest;
import cl.duoc.DisGasBackend.dto.Dtos.RefreshRequest;
import cl.duoc.DisGasBackend.dto.Dtos.RegistroRequest;
import cl.duoc.DisGasBackend.model.Usuario;
import cl.duoc.DisGasBackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Autenticacion", description = "Registro, login y renovacion de token (rutas publicas)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un cliente", description = "Crea una cuenta con rol CLIENTE. 409 si el correo ya existe.")
    public Usuario registro(@Valid @RequestBody RegistroRequest req) {
        return authService.registrar(req);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion", description = "Devuelve token (60 min) y refreshToken (7 dias). 401 si las credenciales son incorrectas.")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar tokens", description = "Recibe un refreshToken valido y entrega un token nuevo y un refreshToken nuevo.")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refrescar(req.refreshToken());
    }
}
