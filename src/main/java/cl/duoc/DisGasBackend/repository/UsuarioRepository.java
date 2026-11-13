package cl.duoc.DisGasBackend.repository;

import cl.duoc.DisGasBackend.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** REPOSITORY de la tabla USUARIO. findByCorreo() = SELECT ... WHERE correo = ?. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}
