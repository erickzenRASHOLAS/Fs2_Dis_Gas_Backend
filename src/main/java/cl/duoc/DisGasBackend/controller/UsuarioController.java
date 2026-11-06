package cl.duoc.DisGasBackend.controller;

import cl.duoc.DisGasBackend.config.OpenApiConfig;
import cl.duoc.DisGasBackend.dto.CambioPasswordRequest;
import cl.duoc.DisGasBackend.dto.EstadoRequest;
import cl.duoc.DisGasBackend.dto.PerfilRequest;
import cl.duoc.DisGasBackend.dto.UsuarioRequest;
import cl.duoc.DisGasBackend.dto.UsuarioResponse;
import cl.duoc.DisGasBackend.model.Rol;
import cl.duoc.DisGasBackend.security.SecurityUtils;
import cl.duoc.DisGasBackend.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/usuarios")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@Tag(name = "Usuarios", description = "Perfil propio (cualquier usuario autenticado) y administración de usuarios (solo ADMINISTRADOR)")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // ----- Perfil propio -----

    @GetMapping("/me")
    @Operation(summary = "Ver mi perfil")
    public UsuarioResponse miPerfil() {
        return usuarioService.perfil(SecurityUtils.actorActual());
    }

    @PutMapping("/me")
    @Operation(summary = "Editar mi perfil", description = "No permite cambiar rol ni estado.")
    public UsuarioResponse editarMiPerfil(@Valid @RequestBody PerfilRequest req) {
        return usuarioService.actualizarPerfil(SecurityUtils.actorActual(), req);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambiar mi contraseña", description = "Requiere la contraseña actual.")
    public void cambiarPassword(@Valid @RequestBody CambioPasswordRequest req) {
        usuarioService.cambiarPassword(SecurityUtils.actorActual(), req);
    }

    // ----- Administración -----

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Listar usuarios (ADMINISTRADOR)", description = "Filtro opcional por rol, por ejemplo ?rol=REPARTIDOR.")
    public List<UsuarioResponse> listar(@RequestParam(required = false) Rol rol) {
        return usuarioService.listar(rol);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Obtener un usuario (ADMINISTRADOR)")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Crear un usuario con cualquier rol (ADMINISTRADOR)")
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest req) {
        return usuarioService.crear(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualizar un usuario (ADMINISTRADOR)", description = "Si se envía password, se reemplaza; si no, se mantiene.")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest req) {
        return usuarioService.actualizar(id, req, SecurityUtils.actorActual());
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Activar o desactivar un usuario (ADMINISTRADOR)")
    public UsuarioResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest req) {
        return usuarioService.cambiarEstado(id, req.estado(), SecurityUtils.actorActual());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Eliminar un usuario (ADMINISTRADOR)", description = "409 si tiene boletas asociadas o si es la propia cuenta.")
    public void eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id, SecurityUtils.actorActual());
    }
}
