package cl.duoc.DisGasBackend.dto;

import cl.duoc.DisGasBackend.model.Estado;
import jakarta.validation.constraints.NotNull;

public record EstadoRequest(@NotNull(message = "El estado es obligatorio") Estado estado) {
}
