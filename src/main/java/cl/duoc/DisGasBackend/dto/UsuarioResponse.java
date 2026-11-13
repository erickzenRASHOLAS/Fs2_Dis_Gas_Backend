package cl.duoc.DisGasBackend.dto;

import cl.duoc.DisGasBackend.model.Estado;
import cl.duoc.DisGasBackend.model.Rol;
import cl.duoc.DisGasBackend.model.Usuario;

/** Vista publica del usuario: nunca incluye la contrasena ni su hash. */
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String telefono,
        String direccion,
        Rol rol,
        Estado estado) {

    public static UsuarioResponse from(Usuario u) {
        Estado estado = u.isActivo() ? Estado.ACTIVO : Estado.INACTIVO;
        return new UsuarioResponse(
                u.getId(),
                u.getNombre(),
                u.getCorreo(),
                u.getTelefono(),
                u.getDireccion(),
                u.getRol(),
                estado);
    }
}
