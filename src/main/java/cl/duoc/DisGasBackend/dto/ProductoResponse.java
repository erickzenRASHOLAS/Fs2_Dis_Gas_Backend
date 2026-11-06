package cl.duoc.DisGasBackend.dto;

import cl.duoc.DisGasBackend.model.Estado;
import cl.duoc.DisGasBackend.model.Producto;

public record ProductoResponse(
        Long id,
        String codigo,
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
        return new ProductoResponse(p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                p.getStock(), p.getStockMinimo(), p.getStock() <= p.getStockMinimo(), p.getEstado(),
                p.getCategoria().getId(), p.getCategoria().getNombre());
    }
}
