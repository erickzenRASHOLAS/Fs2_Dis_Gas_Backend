package cl.duoc.DisGasBackend.repository;

import cl.duoc.DisGasBackend.model.Boleta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {

    List<Boleta> findAllByOrderByFechaDesc();

    List<Boleta> findByUsuarioCorreoOrderByFechaDesc(String correo);

    boolean existsByUsuarioId(Long usuarioId);
}
