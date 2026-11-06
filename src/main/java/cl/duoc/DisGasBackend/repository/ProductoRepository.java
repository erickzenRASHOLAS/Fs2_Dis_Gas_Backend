package cl.duoc.DisGasBackend.repository;

import cl.duoc.DisGasBackend.model.Producto;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @EntityGraph(attributePaths = "categoria")
    List<Producto> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findById(Long id);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    long countByCategoriaId(Long categoriaId);

    /** Bloqueo pesimista: evita vender más unidades que el stock cuando dos pedidos llegan a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id in :ids order by p.id")
    List<Producto> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);
}
