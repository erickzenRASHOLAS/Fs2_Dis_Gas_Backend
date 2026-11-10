package cl.duoc.DisGasBackend.service;

import cl.duoc.DisGasBackend.model.Categoria;
import cl.duoc.DisGasBackend.model.Producto;
import cl.duoc.DisGasBackend.repository.CategoriaRepository;
import cl.duoc.DisGasBackend.repository.DetalleBoletaRepository;
import cl.duoc.DisGasBackend.repository.ProductoRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final DetalleBoletaRepository detalleBoletaRepository;

    public ProductoService(ProductoRepository productoRepository, CategoriaRepository categoriaRepository,
                           DetalleBoletaRepository detalleBoletaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.detalleBoletaRepository = detalleBoletaRepository;
    }

    /** Lo que ve la tienda: solo productos activos. */
    public List<Producto> listarActivos() {
        return productoRepository.findByActivoTrue();
    }

    /** Lo que ven ADMIN y VENDEDOR: todos, tambien los inactivos. */
    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    public Producto obtener(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    public Producto crear(Producto producto) {
        producto.setId(null);
        producto.setCategoria(buscarCategoria(producto));
        return productoRepository.save(producto);
    }

    public Producto actualizar(Long id, Producto datos) {
        Producto producto = obtener(id);
        producto.setNombre(datos.getNombre());
        producto.setDescripcion(datos.getDescripcion());
        producto.setPrecio(datos.getPrecio());
        producto.setStock(datos.getStock());
        producto.setStockMinimo(datos.getStockMinimo());
        producto.setActivo(datos.isActivo());
        producto.setCategoria(buscarCategoria(datos));
        return productoRepository.save(producto);
    }

    public void eliminar(Long id) {
        Producto producto = obtener(id);
        // Regla de negocio: si ya se vendio, se conserva (para no romper las boletas); se puede desactivar
        if (detalleBoletaRepository.existsByProductoId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El producto aparece en boletas y no se puede eliminar; desactivalo (activo = false)");
        }
        productoRepository.delete(producto);
    }

    /** La categoria del JSON solo trae el id; se busca la categoria real en la base de datos. */
    private Categoria buscarCategoria(Producto producto) {
        Long categoriaId = producto.getCategoria().getId();
        if (categoriaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el id de la categoria");
        }
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoria no existe"));
    }
}
