package cl.duoc.DisGasBackend.dto;

import cl.duoc.DisGasBackend.model.Estado;
import cl.duoc.DisGasBackend.model.Rol;
import cl.duoc.DisGasBackend.model.Usuario;
import java.time.LocalDateTime;

/** Vista pública del usuario: nunca incluye la contraseña ni su hash. */
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String telefono,
        String direccion,
        Rol rol,
        Estado estado,
        LocalDateTime fechaCreacion) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getTelefono(), u.getDireccion(),
                u.getRol(), u.getEstado(), u.getFechaCreacion());
    }
}
