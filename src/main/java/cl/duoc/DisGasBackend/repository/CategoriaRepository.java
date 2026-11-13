package cl.duoc.DisGasBackend.repository;

import cl.duoc.DisGasBackend.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

/** REPOSITORY = acceso a la tabla CATEGORIA. Spring Data JPA crea la implementacion; los metodos "existsBy..." generan el SQL solos. */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNombre(String nombre);
}
