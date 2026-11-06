package cl.duoc.DisGasBackend.repository;

import cl.duoc.DisGasBackend.model.Rol;
import cl.duoc.DisGasBackend.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Long id);

    boolean existsByRol(Rol rol);

    List<Usuario> findAllByOrderByNombreAsc();

    List<Usuario> findByRolOrderByNombreAsc(Rol rol);
}
