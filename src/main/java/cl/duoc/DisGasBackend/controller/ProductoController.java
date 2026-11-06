package cl.duoc.DisGasBackend.controller;

import cl.duoc.DisGasBackend.config.OpenApiConfig;
import cl.duoc.DisGasBackend.dto.ProductoRequest;
import cl.duoc.DisGasBackend.dto.ProductoResponse;
import cl.duoc.DisGasBackend.dto.StockRequest;
import cl.duoc.DisGasBackend.security.SecurityUtils;
import cl.duoc.DisGasBackend.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/productos")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Catálogo (lectura pública) e inventario (escritura solo ADMINISTRADOR)")
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    @Operation(summary = "Listar productos (público)",
            description = "Público y clientes ven solo productos ACTIVOS; ADMINISTRADOR y VENDEDOR ven también los inactivos. Filtro opcional ?categoriaId=.")
    public List<ProductoResponse> listar(@RequestParam(required = false) Long categoriaId) {
        return productoService.listar(categoriaId, SecurityUtils.actorOpcional());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un producto (público)")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id, SecurityUtils.actorOpcional());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
    @Operation(summary = "Crear producto (ADMINISTRADOR)")
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest req) {
        return productoService.crear(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
    @Operation(summary = "Actualizar producto (ADMINISTRADOR)")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return productoService.actualizar(id, req);
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
    @Operation(summary = "Actualizar solo el stock (ADMINISTRADOR)", description = "Usado por la pantalla de inventario.")
    public ProductoResponse actualizarStock(@PathVariable Long id, @Valid @RequestBody StockRequest req) {
        return productoService.actualizarStock(id, req.stock());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
    @Operation(summary = "Eliminar producto (ADMINISTRADOR)", description = "409 si el producto ya aparece en boletas (en ese caso márcalo INACTIVO).")
    public void eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
    }
}
