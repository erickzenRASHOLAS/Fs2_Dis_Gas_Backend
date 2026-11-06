package cl.duoc.DisGasBackend.dto;

import cl.duoc.DisGasBackend.model.Estado;
import cl.duoc.DisGasBackend.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta/edición de usuarios por un administrador. La contraseña es obligatoria al crear y opcional al editar. */
public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo inválido") @Size(max = 120) String correo,
        @NotBlank(message = "El teléfono es obligatorio") @Size(max = 20) String telefono,
        @NotBlank(message = "La dirección es obligatoria") @Size(max = 200) String direccion,
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres") String password,
        @NotNull(message = "El rol es obligatorio") Rol rol,
        Estado estado) {
}
