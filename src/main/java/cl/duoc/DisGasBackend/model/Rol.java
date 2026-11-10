package cl.duoc.DisGasBackend.model;

/** Los 3 roles que pide la pauta. */
public enum Rol {
    ADMINISTRADOR,  // acceso total
    VENDEDOR,       // solo ve productos y boletas
    CLIENTE         // solo la tienda: compra y ve sus boletas
}
