package cl.duoc.DisGasBackend.dto;

import cl.duoc.DisGasBackend.model.EstadoBoleta;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoBoletaRequest(@NotNull(message = "El estado es obligatorio") EstadoBoleta estado) {
}
