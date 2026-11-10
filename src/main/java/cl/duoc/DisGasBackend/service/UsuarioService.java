package cl.duoc.DisGasBackend.service;

import cl.duoc.DisGasBackend.dto.Dtos.PerfilRequest;
import cl.duoc.DisGasBackend.dto.Dtos.UsuarioRequest;
import cl.duoc.DisGasBackend.model.Usuario;
import cl.duoc.DisGasBackend.repository.BoletaRepository;
import cl.duoc.DisGasBackend.repository.UsuarioRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BoletaRepository boletaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, BoletaRepository boletaRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.boletaRepository = boletaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------- Administracion (solo ADMINISTRADOR) ----------

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public Usuario obtener(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    public Usuario crear(UsuarioRequest req) {
        if (req.password() == null || req.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria al crear un usuario");
        }
        String correo = req.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya esta registrado");
        }
        Usuario u = new Usuario();
        u.setCorreo(correo);
        u.setPassword(passwordEncoder.encode(req.password()));
        copiarDatos(u, req);
        return usuarioRepository.save(u);
    }

    public Usuario actualizar(Long id, UsuarioRequest req) {
        Usuario u = obtener(id);
        String correo = req.correo().trim().toLowerCase();
        // El correo nuevo no puede pertenecer a OTRO usuario
        boolean correoDeOtro = usuarioRepository.findByCorreo(correo)
                .filter(otro -> !otro.getId().equals(id))
                .isPresent();
        if (correoDeOtro) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese correo ya pertenece a otro usuario");
        }
        u.setCorreo(correo);
        if (req.password() != null && !req.password().isBlank()) {
            u.setPassword(passwordEncoder.encode(req.password()));   // solo cambia si se envia una nueva
        }
        copiarDatos(u, req);
        return usuarioRepository.save(u);
    }

    public void eliminar(Long id, String correoDelAdmin) {
        Usuario u = obtener(id);
        if (u.getCorreo().equals(correoDelAdmin)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes eliminar tu propia cuenta");
        }
        if (boletaRepository.existsByUsuarioId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario tiene boletas asociadas; desactivalo (activo = false)");
        }
        usuarioRepository.delete(u);
    }

    // ---------- Perfil propio (cualquier usuario con sesion) ----------

    public Usuario perfil(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    public Usuario actualizarPerfil(String correo, PerfilRequest req) {
        Usuario u = perfil(correo);
        u.setNombre(req.nombre());
        u.setTelefono(req.telefono());
        u.setDireccion(req.direccion());
        return usuarioRepository.save(u);
    }

    private void copiarDatos(Usuario u, UsuarioRequest req) {
        u.setNombre(req.nombre());
        u.setTelefono(req.telefono());
        u.setDireccion(req.direccion());
        u.setRol(req.rol());
        if (req.activo() != null) {
            u.setActivo(req.activo());
        }
    }
}
