package cl.duoc.DisGasBackend.dto;

import cl.duoc.DisGasBackend.model.DetalleBoleta;

public record DetalleBoletaResponse(
        Long productoId,
        String productoCodigo,
        String productoNombre,
        Integer cantidad,
        Integer precioUnitario,
        Integer subtotal) {

    public static DetalleBoletaResponse from(DetalleBoleta d) {
        return new DetalleBoletaResponse(d.getProducto().getId(), d.getProducto().getCodigo(),
                d.getProducto().getNombre(), d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal());
    }
}
