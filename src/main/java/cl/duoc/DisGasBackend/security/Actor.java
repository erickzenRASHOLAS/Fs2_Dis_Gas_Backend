package cl.duoc.DisGasBackend.security;

import cl.duoc.DisGasBackend.model.Rol;

/** Usuario que ejecuta la operación (extraído del JWT). Los servicios reciben esto en vez de depender de Spring Security. */
public record Actor(Long id, Rol rol) {

    public boolean es(Rol esperado) {
        return rol == esperado;
    }
}
