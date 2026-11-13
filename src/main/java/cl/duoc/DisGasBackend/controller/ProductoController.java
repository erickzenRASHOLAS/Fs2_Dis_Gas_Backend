package cl.duoc.DisGasBackend.controller;

import cl.duoc.DisGasBackend.model.Producto;
import cl.duoc.DisGasBackend.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CONTROLLER de productos e inventario. @RequestMapping("/v1/productos") = todas las rutas empiezan con /v1/productos. */
@RestController
@RequestMapping("/v1/productos")
@Tag(name = "Productos", description = "Catalogo e inventario. Lectura publica; crear/editar/eliminar solo ADMINISTRADOR")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @Operation(summary = "Listar productos activos (publico)", description = "Es el catalogo de la tienda.")
    public List<Producto> listar() {
        return productoService.listarActivos();
    }

    @GetMapping("/todos")
    @Operation(summary = "Listar TODOS los productos, incluso inactivos (ADMINISTRADOR, VENDEDOR)")
    public List<Producto> listarTodos() {
        return productoService.listarTodos();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un producto (publico)")
    public Producto obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear producto (ADMINISTRADOR)", description = "La categoria se envia como { \"id\": 1 }.")
    public Producto crear(@Valid @RequestBody Producto producto) {
        return productoService.crear(producto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto o su stock (ADMINISTRADOR)")
    public Producto actualizar(@PathVariable Long id, @Valid @RequestBody Producto producto) {
        return productoService.actualizar(id, producto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar producto (ADMINISTRADOR)", description = "409 si el producto ya aparece en boletas.")
    public void eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
    }
}
