package cl.duoc.DisGasBackend.dto;

import cl.duoc.DisGasBackend.model.Estado;
import cl.duoc.DisGasBackend.model.Producto;

/** Respuesta de producto. El estado se deriva del atributo activo de la entidad. */
public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        Integer precio,
        Integer stock,
        Integer stockMinimo,
        boolean bajoMinimo,
        Estado estado,
        Long categoriaId,
        String categoriaNombre) {

    public static ProductoResponse from(Producto p) {
        Estado estado = p.isActivo() ? Estado.ACTIVO : Estado.INACTIVO;
        return new ProductoResponse(
                p.getId(),
                p.getNombre(),
                p.getDescripcion(),
                p.getPrecio(),
                p.getStock(),
                p.getStockMinimo(),
                p.getStock() <= p.getStockMinimo(),
                estado,
                p.getCategoria().getId(),
                p.getCategoria().getNombre());
    }
}
