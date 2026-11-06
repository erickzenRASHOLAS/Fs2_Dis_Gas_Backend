package cl.duoc.DisGasBackend.dto;

import jakarta.validation.constraints.NotNull;

public record AsignarRepartidorRequest(@NotNull(message = "El repartidor es obligatorio") Long repartidorId) {
}
