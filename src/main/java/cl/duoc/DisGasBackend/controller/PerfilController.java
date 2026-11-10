package cl.duoc.DisGasBackend.controller;

import cl.duoc.DisGasBackend.dto.Dtos.PerfilRequest;
import cl.duoc.DisGasBackend.model.Usuario;
import cl.duoc.DisGasBackend.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/perfil")
@Tag(name = "Perfil", description = "Datos del usuario con sesion iniciada (cualquier rol)")
public class PerfilController {

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Ver mi perfil")
    public Usuario verPerfil(Authentication authentication) {
        return usuarioService.perfil(authentication.getName());
    }

    @PutMapping
    @Operation(summary = "Editar mi perfil", description = "Solo nombre, telefono y direccion. No permite cambiar rol.")
    public Usuario editarPerfil(Authentication authentication, @Valid @RequestBody PerfilRequest req) {
        return usuarioService.actualizarPerfil(authentication.getName(), req);
    }
}
