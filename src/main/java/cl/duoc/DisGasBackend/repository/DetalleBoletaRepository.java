package cl.duoc.DisGasBackend.repository;

import cl.duoc.DisGasBackend.model.DetalleBoleta;
import org.springframework.data.jpa.repository.JpaRepository;

/** REPOSITORY de la tabla DETALLE_BOLETA. Sirve para saber si un producto ya fue vendido. */
public interface DetalleBoletaRepository extends JpaRepository<DetalleBoleta, Long> {

    boolean existsByProductoId(Long productoId);
}
