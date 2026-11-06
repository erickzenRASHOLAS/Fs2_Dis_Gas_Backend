package cl.duoc.DisGasBackend.exception;

/** 400: los datos enviados incumplen una regla de negocio. */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
