package cl.duoc.DisGasBackend.dto;

import cl.duoc.DisGasBackend.model.EstadoBoleta;
import cl.duoc.DisGasBackend.model.Rol;
import cl.duoc.DisGasBackend.model.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTOs = los "formularios" que llegan en el cuerpo (JSON) de las peticiones.
 * Todos estan juntos en este archivo para estudiarlos facil.
 * Las anotaciones (@NotBlank, @Email...) validan los datos antes de llegar a la logica de negocio.
 */
public class Dtos {

    private Dtos() {
    }

    // ---------- Autenticacion ----------

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo invalido") String correo,
            @NotBlank(message = "La contraseña es obligatoria") String password) {
    }

    public record RegistroRequest(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo invalido") String correo,
            @NotBlank(message = "El telefono es obligatorio") String telefono,
            @NotBlank(message = "La direccion es obligatoria") String direccion,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres") String password) {
    }

    public record RefreshRequest(
            @NotBlank(message = "El refresh token es obligatorio") String refreshToken) {
    }

    /** Respuesta del login: token de acceso (corto), token de renovacion (largo) y datos del usuario. */
    public record AuthResponse(String token, String refreshToken, Usuario usuario) {
    }

    // ---------- Usuarios ----------

    /** Lo usa el administrador para crear/editar usuarios. En la edicion la password es opcional. */
    public record UsuarioRequest(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo invalido") String correo,
            String telefono,
            String direccion,
            @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres") String password,
            @NotNull(message = "El rol es obligatorio") Rol rol,
            Boolean activo) {
    }

    /** Datos que cada usuario puede cambiar de su propio perfil. */
    public record PerfilRequest(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            String telefono,
            String direccion) {
    }

    // ---------- Boletas ----------

    public record ItemRequest(
            @NotNull(message = "El producto es obligatorio") Long productoId,
            @NotNull(message = "La cantidad es obligatoria") @Min(value = 1, message = "La cantidad minima es 1") Integer cantidad) {
    }

    /** El cliente solo envia producto y cantidad. Los precios y el total los calcula el servidor. */
    public record BoletaRequest(
            @NotBlank(message = "La direccion de despacho es obligatoria") String direccion,
            @NotEmpty(message = "Debes agregar al menos un producto") @Valid List<ItemRequest> items) {
    }

    public record EstadoRequest(
            @NotNull(message = "El estado es obligatorio") EstadoBoleta estado) {
    }
}
