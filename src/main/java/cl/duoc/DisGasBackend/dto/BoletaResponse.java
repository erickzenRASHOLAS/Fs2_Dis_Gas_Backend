package cl.duoc.DisGasBackend.dto;

import cl.duoc.DisGasBackend.model.Boleta;
import cl.duoc.DisGasBackend.model.EstadoBoleta;
import cl.duoc.DisGasBackend.model.EstadoPago;
import java.time.LocalDateTime;
import java.util.List;

public record BoletaResponse(
        Long id,
        String numero,
        Long clienteId,
        String nombreCliente,
        String telefono,
        String direccion,
        LocalDateTime fecha,
        EstadoBoleta estado,
        Integer total,
        Long repartidorId,
        String repartidorNombre,
        EstadoPago estadoPago,
        String metodoPago,
        Integer montoPagado,
        LocalDateTime fechaPago,
        List<DetalleBoletaResponse> detalles) {

    public static BoletaResponse from(Boleta b) {
        return new BoletaResponse(
                b.getId(),
                String.format("ORD-%05d", b.getId()),
                b.getUsuario().getId(),
                b.getNombreCliente(),
                b.getTelefono(),
                b.getDireccion(),
                b.getFecha(),
                b.getEstado(),
                b.getTotal(),
                b.getRepartidor() != null ? b.getRepartidor().getId() : null,
                b.getRepartidor() != null ? b.getRepartidor().getNombre() : null,
                b.getEstadoPago(),
                b.getMetodoPago(),
                b.getMontoPagado(),
                b.getFechaPago(),
                b.getDetalles().stream().map(DetalleBoletaResponse::from).toList());
    }
}
