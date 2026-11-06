package cl.duoc.DisGasBackend.service;

import cl.duoc.DisGasBackend.dto.CambioPasswordRequest;
import cl.duoc.DisGasBackend.dto.PerfilRequest;
import cl.duoc.DisGasBackend.dto.UsuarioRequest;
import cl.duoc.DisGasBackend.dto.UsuarioResponse;
import cl.duoc.DisGasBackend.exception.ConflictoException;
import cl.duoc.DisGasBackend.exception.RecursoNoEncontradoException;
import cl.duoc.DisGasBackend.exception.SolicitudInvalidaException;
import cl.duoc.DisGasBackend.model.Estado;
import cl.duoc.DisGasBackend.model.Rol;
import cl.duoc.DisGasBackend.model.Usuario;
import cl.duoc.DisGasBackend.repository.BoletaRepository;
import cl.duoc.DisGasBackend.repository.UsuarioRepository;
import cl.duoc.DisGasBackend.security.Actor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BoletaRepository boletaRepository;
    private final PasswordEncoder passwordEncoder;

    // ---------- Administración (solo ADMINISTRADOR) ----------

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Rol rol) {
        List<Usuario> lista = rol == null
                ? usuarioRepository.findAllByOrderByNombreAsc()
                : usuarioRepository.findByRolOrderByNombreAsc(rol);
        return lista.stream().map(UsuarioResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.from(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest req) {
        if (req.password() == null || req.password().isBlank()) {
            throw new SolicitudInvalidaException("La contraseña es obligatoria (mínimo 6 caracteres)");
        }
        String correo = normalizar(req.correo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictoException("El correo electrónico ya se encuentra registrado");
        }
        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setCorreo(correo);
        u.setTelefono(req.telefono().trim());
        u.setDireccion(req.direccion().trim());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRol(req.rol());
        u.setEstado(req.estado() != null ? req.estado() : Estado.ACTIVO);
        return UsuarioResponse.from(usuarioRepository.save(u));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest req, Actor actor) {
        Usuario u = buscar(id);
        String correo = normalizar(req.correo());
        if (usuarioRepository.existsByCorreoAndIdNot(correo, id)) {
            throw new ConflictoException("Ese correo ya pertenece a otro usuario");
        }
        Estado nuevoEstado = req.estado() != null ? req.estado() : u.getEstado();
        if (actor.id().equals(id) && (req.rol() != u.getRol() || nuevoEstado != Estado.ACTIVO)) {
            throw new ConflictoException("No puedes cambiar tu propio rol ni desactivar tu propia cuenta");
        }
        u.setNombre(req.nombre().trim());
        u.setCorreo(correo);
        u.setTelefono(req.telefono().trim());
        u.setDireccion(req.direccion().trim());
        u.setRol(req.rol());
        u.setEstado(nuevoEstado);
        if (req.password() != null && !req.password().isBlank()) {
            u.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        return UsuarioResponse.from(usuarioRepository.save(u));
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, Estado estado, Actor actor) {
        if (actor.id().equals(id) && estado != Estado.ACTIVO) {
            throw new ConflictoException("No puedes desactivar tu propia cuenta");
        }
        Usuario u = buscar(id);
        u.setEstado(estado);
        return UsuarioResponse.from(usuarioRepository.save(u));
    }

    @Transactional
    public void eliminar(Long id, Actor actor) {
        if (actor.id().equals(id)) {
            throw new ConflictoException("No puedes eliminar tu propia cuenta");
        }
        Usuario u = buscar(id);
        if (boletaRepository.existsByUsuarioId(id) || boletaRepository.existsByRepartidorId(id)) {
            throw new ConflictoException("El usuario tiene boletas asociadas; en su lugar puedes desactivarlo");
        }
        usuarioRepository.delete(u);
    }

    // ---------- Perfil propio (cualquier usuario autenticado) ----------

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(Actor actor) {
        return UsuarioResponse.from(buscar(actor.id()));
    }

    @Transactional
    public UsuarioResponse actualizarPerfil(Actor actor, PerfilRequest req) {
        Usuario u = buscar(actor.id());
        String correo = normalizar(req.correo());
        if (usuarioRepository.existsByCorreoAndIdNot(correo, u.getId())) {
            throw new ConflictoException("Ese correo ya pertenece a otro usuario");
        }
        u.setNombre(req.nombre().trim());
        u.setCorreo(correo);
        u.setTelefono(req.telefono().trim());
        u.setDireccion(req.direccion().trim());
        return UsuarioResponse.from(usuarioRepository.save(u));
    }

    @Transactional
    public void cambiarPassword(Actor actor, CambioPasswordRequest req) {
        Usuario u = buscar(actor.id());
        if (!passwordEncoder.matches(req.passwordActual(), u.getPasswordHash())) {
            throw new SolicitudInvalidaException("La contraseña actual no es correcta");
        }
        u.setPasswordHash(passwordEncoder.encode(req.passwordNueva()));
        usuarioRepository.save(u);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado (id " + id + ")"));
    }

    private String normalizar(String correo) {
        return correo.trim().toLowerCase();
    }
}
