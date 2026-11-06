package cl.duoc.DisGasBackend.service;

import cl.duoc.DisGasBackend.dto.CategoriaRequest;
import cl.duoc.DisGasBackend.dto.CategoriaResponse;
import cl.duoc.DisGasBackend.exception.ConflictoException;
import cl.duoc.DisGasBackend.exception.RecursoNoEncontradoException;
import cl.duoc.DisGasBackend.model.Categoria;
import cl.duoc.DisGasBackend.repository.CategoriaRepository;
import cl.duoc.DisGasBackend.repository.ProductoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAllByOrderByNombreAsc().stream().map(CategoriaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return CategoriaResponse.from(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest req) {
        String nombre = req.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe una categoría con el nombre '" + nombre + "'");
        }
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setDescripcion(req.descripcion());
        return CategoriaResponse.from(categoriaRepository.save(c));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest req) {
        Categoria c = buscar(id);
        String nombre = req.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictoException("Ya existe una categoría con el nombre '" + nombre + "'");
        }
        c.setNombre(nombre);
        c.setDescripcion(req.descripcion());
        return CategoriaResponse.from(categoriaRepository.save(c));
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria c = buscar(id);
        if (productoRepository.countByCategoriaId(id) > 0) {
            throw new ConflictoException("La categoría tiene productos asociados y no se puede eliminar");
        }
        categoriaRepository.delete(c);
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada (id " + id + ")"));
    }
}
