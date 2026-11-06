package cl.duoc.DisGasBackend.exception;

/** 409: la operación choca con el estado actual (duplicados, stock insuficiente, transición inválida). */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
