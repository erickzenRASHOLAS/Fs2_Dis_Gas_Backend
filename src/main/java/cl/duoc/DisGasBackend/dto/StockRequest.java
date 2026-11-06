package cl.duoc.DisGasBackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StockRequest(
        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock debe ser un número entero igual o mayor que cero") Integer stock) {
}
