package cl.duoc.DisGasBackend.repository;

import cl.duoc.DisGasBackend.model.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByActivoTrue();

    boolean existsByCategoriaId(Long categoriaId);
}
