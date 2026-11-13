package cl.duoc.DisGasBackend.dto;

import cl.duoc.DisGasBackend.model.DetalleBoleta;

/** Linea de boleta con los datos de producto disponibles en el modelo actual. */
public record DetalleBoletaResponse(
        Long productoId,
        String productoNombre,
        Integer cantidad,
        Integer precioUnitario,
        Integer subtotal) {

    public static DetalleBoletaResponse from(DetalleBoleta d) {
        return new DetalleBoletaResponse(
                d.getProducto().getId(),
                d.getProducto().getNombre(),
                d.getCantidad(),
                d.getPrecioUnitario(),
                d.getSubtotal());
    }
}
