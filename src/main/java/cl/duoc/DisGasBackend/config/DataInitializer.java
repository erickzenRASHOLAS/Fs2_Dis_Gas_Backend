package cl.duoc.DisGasBackend.config;

import cl.duoc.DisGasBackend.model.Categoria;
import cl.duoc.DisGasBackend.model.Producto;
import cl.duoc.DisGasBackend.model.Rol;
import cl.duoc.DisGasBackend.model.Usuario;
import cl.duoc.DisGasBackend.repository.CategoriaRepository;
import cl.duoc.DisGasBackend.repository.ProductoRepository;
import cl.duoc.DisGasBackend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Se ejecuta una vez al iniciar la app. Si las tablas estan vacias, las llena con:
 *  - el catalogo inicial (2 categorias y 8 productos)
 *  - usuarios de demostracion (solo si app.seed.demo-users=true)
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.demo-users}")
    private boolean crearUsuariosDemo;

    public DataInitializer(CategoriaRepository categoriaRepository, ProductoRepository productoRepository,
                           UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (categoriaRepository.count() == 0) {
            cargarCatalogo();
        }
        if (crearUsuariosDemo && usuarioRepository.count() == 0) {
            cargarUsuariosDemo();
        }
    }

    private void cargarCatalogo() {
        Categoria gas = crearCategoria("Gas", "Cilindros de gas licuado");
        Categoria accesorios = crearCategoria("Accesorios", "Conectores y accesorios para gas");

        crearProducto("Cilindro 5 kg", "Cilindro de gas licuado de 5 kg", 10800, 32, 8, gas);
        crearProducto("Cilindro 11 kg", "Cilindro de gas licuado de 11 kg", 21500, 48, 10, gas);
        crearProducto("Cilindro 15 kg", "Cilindro de gas licuado de 15 kg", 27900, 27, 8, gas);
        crearProducto("Regulador de gas", "Regulador de presion para cilindro", 12500, 40, 10, accesorios);
        crearProducto("Manguera para gas 1,5 m", "Manguera flexible para gas de 1,5 metros", 5500, 40, 10, accesorios);
        crearProducto("Conector de gas", "Conector para instalacion de gas", 3500, 60, 10, accesorios);
        crearProducto("Abrazadera para manguera", "Abrazadera metalica para manguera", 1200, 80, 15, accesorios);
        crearProducto("Kit regulador y manguera", "Kit completo de regulador y manguera", 16900, 25, 5, accesorios);
    }

    private void cargarUsuariosDemo() {
        crearUsuario("Admin Volcan", "admin@volcan.cl", "admin123", Rol.ADMINISTRADOR);
        crearUsuario("Ana Vendedora", "ana.vendedora@volcan.cl", "123456", Rol.VENDEDOR);
        crearUsuario("Juan Perez", "juan.perez@email.com", "123456", Rol.CLIENTE);
        crearUsuario("Maria Gonzalez", "maria.gonzalez@email.com", "123456", Rol.CLIENTE);
    }

    private Categoria crearCategoria(String nombre, String descripcion) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setDescripcion(descripcion);
        return categoriaRepository.save(c);
    }

    private void crearProducto(String nombre, String descripcion, int precio, int stock, int stockMinimo,
                               Categoria categoria) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setStock(stock);
        p.setStockMinimo(stockMinimo);
        p.setCategoria(categoria);
        productoRepository.save(p);
    }

    private void crearUsuario(String nombre, String correo, String password, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setTelefono("+56 9 1234 5678");
        u.setDireccion("Chillan");
        u.setPassword(passwordEncoder.encode(password));
        u.setRol(rol);
        usuarioRepository.save(u);
    }
}
