package cl.duoc.DisGasBackend.service;

import cl.duoc.DisGasBackend.dto.ProductoRequest;
import cl.duoc.DisGasBackend.dto.ProductoResponse;
import cl.duoc.DisGasBackend.exception.ConflictoException;
import cl.duoc.DisGasBackend.exception.RecursoNoEncontradoException;
import cl.duoc.DisGasBackend.model.Categoria;
import cl.duoc.DisGasBackend.model.Estado;
import cl.duoc.DisGasBackend.model.Producto;
import cl.duoc.DisGasBackend.model.Rol;
import cl.duoc.DisGasBackend.repository.CategoriaRepository;
import cl.duoc.DisGasBackend.repository.DetalleBoletaRepository;
import cl.duoc.DisGasBackend.repository.ProductoRepository;
import cl.duoc.DisGasBackend.security.Actor;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final DetalleBoletaRepository detalleBoletaRepository;

    /**
     * Catálogo. El público y los clientes solo ven productos ACTIVOS;
     * administrador y vendedor ven también los inactivos.
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(Long categoriaId, Optional<Actor> actor) {
        boolean verTodos = veInactivos(actor);
        return productoRepository.findAllByOrderByNombreAsc().stream()
                .filter(p -> verTodos || p.getEstado() == Estado.ACTIVO)
                .filter(p -> categoriaId == null || p.getCategoria().getId().equals(categoriaId))
                .map(ProductoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id, Optional<Actor> actor) {
        Producto p = buscar(id);
        if (p.getEstado() != Estado.ACTIVO && !veInactivos(actor)) {
            throw new RecursoNoEncontradoException("Producto no encontrado (id " + id + ")");
        }
        return ProductoResponse.from(p);
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest req) {
        String codigo = req.codigo().trim();
        if (productoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictoException("Ya existe un producto con el código '" + codigo + "'");
        }
        Producto p = new Producto();
        aplicar(p, req, codigo);
        return ProductoResponse.from(productoRepository.save(p));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest req) {
        Producto p = buscar(id);
        String codigo = req.codigo().trim();
        if (productoRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new ConflictoException("Ya existe un producto con el código '" + codigo + "'");
        }
        aplicar(p, req, codigo);
        return ProductoResponse.from(productoRepository.save(p));
    }

    @Transactional
    public ProductoResponse actualizarStock(Long id, Integer stock) {
        Producto p = buscar(id);
        p.setStock(stock);
        return ProductoResponse.from(productoRepository.save(p));
    }

    @Transactional
    public void eliminar(Long id) {
        Producto p = buscar(id);
        if (detalleBoletaRepository.existsByProductoId(id)) {
            throw new ConflictoException("El producto aparece en boletas y no se puede eliminar; puedes marcarlo como INACTIVO");
        }
        productoRepository.delete(p);
    }

    private void aplicar(Producto p, ProductoRequest req, String codigo) {
        Categoria categoria = categoriaRepository.findById(req.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada (id " + req.categoriaId() + ")"));
        p.setCodigo(codigo);
        p.setNombre(req.nombre().trim());
        p.setDescripcion(req.descripcion());
        p.setPrecio(req.precio());
        p.setStock(req.stock());
        p.setStockMinimo(req.stockMinimo() != null ? req.stockMinimo() : 0);
        p.setEstado(req.estado() != null ? req.estado() : Estado.ACTIVO);
        p.setCategoria(categoria);
    }

    private boolean veInactivos(Optional<Actor> actor) {
        return actor.map(a -> a.rol() == Rol.ADMINISTRADOR || a.rol() == Rol.VENDEDOR).orElse(false);
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado (id " + id + ")"));
    }
}
