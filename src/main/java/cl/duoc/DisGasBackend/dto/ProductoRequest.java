package cl.duoc.DisGasBackend.dto;

import cl.duoc.DisGasBackend.model.Estado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
        @NotBlank(message = "El código es obligatorio") @Size(max = 40) String codigo,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
        @Size(max = 255) String descripcion,
        @NotNull(message = "El precio es obligatorio") @PositiveOrZero(message = "El precio no puede ser negativo") Integer precio,
        @NotNull(message = "El stock es obligatorio") @PositiveOrZero(message = "El stock no puede ser negativo") Integer stock,
        @PositiveOrZero(message = "El stock mínimo no puede ser negativo") Integer stockMinimo,
        @NotNull(message = "La categoría es obligatoria") Long categoriaId,
        Estado estado) {
}
