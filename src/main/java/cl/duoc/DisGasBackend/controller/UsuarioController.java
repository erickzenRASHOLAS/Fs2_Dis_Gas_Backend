package cl.duoc.DisGasBackend.controller;

import cl.duoc.DisGasBackend.dto.Dtos.UsuarioRequest;
import cl.duoc.DisGasBackend.model.Usuario;
import cl.duoc.DisGasBackend.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CONTROLLER de administracion de usuarios (solo ADMINISTRADOR). */
@RestController
@RequestMapping("/v1/usuarios")
@Tag(name = "Usuarios", description = "Administracion de usuarios: solo ADMINISTRADOR")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios (ADMINISTRADOR)")
    public List<Usuario> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un usuario (ADMINISTRADOR)")
    public Usuario obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear usuario con cualquier rol (ADMINISTRADOR)")
    public Usuario crear(@Valid @RequestBody UsuarioRequest req) {
        return usuarioService.crear(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario (ADMINISTRADOR)", description = "Si no se envia password, se mantiene la actual. Con activo = false se desactiva la cuenta.")
    public Usuario actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest req) {
        return usuarioService.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar usuario (ADMINISTRADOR)", description = "409 si tiene boletas o si es tu propia cuenta.")
    public void eliminar(@PathVariable Long id, Authentication authentication) {
        // authentication.getName() es el correo guardado dentro del token JWT
        usuarioService.eliminar(id, authentication.getName());
    }
}
