package cl.duoc.DisGasBackend.config;

import cl.duoc.DisGasBackend.model.Estado;
import cl.duoc.DisGasBackend.model.Rol;
import cl.duoc.DisGasBackend.model.Usuario;
import cl.duoc.DisGasBackend.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Carga de usuarios al iniciar:
 * - app.seed.demo-users=true  -> cuentas de demostración (solo desarrollo; en prod está en false).
 * - ADMIN_EMAIL + ADMIN_PASSWORD -> crea el primer administrador si todavía no existe ninguno.
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean demo;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.seed.demo-users:false}") boolean demo,
                           @Value("${app.seed.admin-email:}") String adminEmail,
                           @Value("${app.seed.admin-password:}") String adminPassword) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.demo = demo;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (demo) {
            crear("Juan Pérez", "juan.perez@email.com", "+56 9 1234 5678", "Av. Brasil 456, Depto 12, Chillán", "123456", Rol.CLIENTE);
            crear("María González", "maria.gonzalez@email.com", "+56 9 8765 4321", "Los Pinos 123, Chillán", "123456", Rol.CLIENTE);
            crear("Carlos Repartidor", "carlos.repartidor@volcan.cl", "+56 9 5555 4444", "Base Central, Chillán", "123456", Rol.REPARTIDOR);
            crear("Ana Vendedora", "ana.vendedora@volcan.cl", "+56 9 3333 2222", "Oficina Central, Chillán", "123456", Rol.VENDEDOR);
            crear("Admin Volcán", "admin@volcan.cl", "+56 9 9999 8888", "Oficina Central, Chillán", "admin123", Rol.ADMINISTRADOR);
            log.warn("Usuarios de DEMOSTRACIÓN cargados. No usar en producción (app.seed.demo-users=false).");
        }
        if (!adminEmail.isBlank() && !adminPassword.isBlank() && !usuarioRepository.existsByRol(Rol.ADMINISTRADOR)) {
            crear("Administrador", adminEmail, "-", "-", adminPassword, Rol.ADMINISTRADOR);
            log.info("Administrador inicial creado: {}", adminEmail);
        }
    }

    private void crear(String nombre, String correo, String telefono, String direccion, String password, Rol rol) {
        String correoNormalizado = correo.trim().toLowerCase();
        if (usuarioRepository.existsByCorreo(correoNormalizado)) {
            return;
        }
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(correoNormalizado);
        u.setTelefono(telefono);
        u.setDireccion(direccion);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRol(rol);
        u.setEstado(Estado.ACTIVO);
        usuarioRepository.save(u);
    }
}
