package cl.duoc.DisGasBackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Pedido del cliente. El total y los precios los calcula el servidor; el cliente solo envía producto y cantidad. */
public record BoletaRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombreCliente,
        @NotBlank(message = "El teléfono es obligatorio") @Size(max = 20) String telefono,
        @NotBlank(message = "La dirección de despacho es obligatoria") @Size(max = 200) String direccion,
        @NotEmpty(message = "Debes agregar al menos un producto") @Valid List<ItemBoletaRequest> items) {
}
