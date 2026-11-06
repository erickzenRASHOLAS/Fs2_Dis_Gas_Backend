package cl.duoc.DisGasBackend.exception;

/** 404: el recurso solicitado no existe. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
