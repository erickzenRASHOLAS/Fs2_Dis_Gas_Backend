package cl.duoc.DisGasBackend.repository;

import cl.duoc.DisGasBackend.model.Boleta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {

    @EntityGraph(attributePaths = {"usuario", "repartidor", "detalles", "detalles.producto"})
    Optional<Boleta> findById(Long id);

    @EntityGraph(attributePaths = {"usuario", "repartidor", "detalles", "detalles.producto"})
    List<Boleta> findAllByOrderByFechaDescIdDesc();

    @EntityGraph(attributePaths = {"usuario", "repartidor", "detalles", "detalles.producto"})
    List<Boleta> findByUsuarioIdOrderByFechaDescIdDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario", "repartidor", "detalles", "detalles.producto"})
    List<Boleta> findByRepartidorIdOrderByFechaDescIdDesc(Long repartidorId);

    boolean existsByUsuarioId(Long usuarioId);

    boolean existsByRepartidorId(Long repartidorId);
}
