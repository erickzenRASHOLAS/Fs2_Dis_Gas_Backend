package cl.duoc.DisGasBackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo inválido") @Size(max = 120) String correo,
        @NotBlank(message = "El teléfono es obligatorio") @Size(max = 20) String telefono,
        @NotBlank(message = "La dirección es obligatoria") @Size(max = 200) String direccion,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres") String password) {
}
