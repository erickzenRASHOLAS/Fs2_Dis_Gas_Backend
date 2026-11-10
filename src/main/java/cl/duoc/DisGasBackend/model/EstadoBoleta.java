package cl.duoc.DisGasBackend.model;

/** Flujo: PENDIENTE -> EN_RUTA -> ENTREGADO. CANCELADO anula la boleta y devuelve el stock. */
public enum EstadoBoleta {
    PENDIENTE,
    EN_RUTA,
    ENTREGADO,
    CANCELADO
}
