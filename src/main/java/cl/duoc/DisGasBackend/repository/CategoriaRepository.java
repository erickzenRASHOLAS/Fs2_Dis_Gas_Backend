package cl.duoc.DisGasBackend.repository;

import cl.duoc.DisGasBackend.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNombre(String nombre);
}
