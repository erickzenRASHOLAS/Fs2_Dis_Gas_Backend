package cl.duoc.DisGasBackend.controller;

import cl.duoc.DisGasBackend.config.OpenApiConfig;
import cl.duoc.DisGasBackend.dto.AsignarRepartidorRequest;
import cl.duoc.DisGasBackend.dto.BoletaRequest;
import cl.duoc.DisGasBackend.dto.BoletaResponse;
import cl.duoc.DisGasBackend.dto.CambioEstadoBoletaRequest;
import cl.duoc.DisGasBackend.model.EstadoBoleta;
import cl.duoc.DisGasBackend.security.SecurityUtils;
import cl.duoc.DisGasBackend.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/boletas")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
@Tag(name = "Boletas (pedidos)", description = "Cada rol ve solo lo que le corresponde: CLIENTE las suyas, REPARTIDOR las asignadas, ADMINISTRADOR y VENDEDOR todas")
public class BoletaController {

    private final BoletaService boletaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENTE','ADMINISTRADOR')")
    @Operation(summary = "Crear un pedido (CLIENTE, ADMINISTRADOR)",
            description = "El servidor calcula precios y total, valida stock y lo descuenta. 409 si no hay stock suficiente.")
    public BoletaResponse crear(@Valid @RequestBody BoletaRequest req) {
        return boletaService.crear(SecurityUtils.actorActual(), req);
    }

    @GetMapping
    @Operation(summary = "Listar boletas según el rol", description = "Filtro opcional ?estado=PENDIENTE|EN_RUTA|ENTREGADO|CANCELADO.")
    public List<BoletaResponse> listar(@RequestParam(required = false) EstadoBoleta estado) {
        return boletaService.listar(SecurityUtils.actorActual(), estado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una boleta", description = "403 si la boleta no pertenece al usuario (cliente) ni está asignada a él (repartidor).")
    public BoletaResponse obtener(@PathVariable Long id) {
        return boletaService.obtener(id, SecurityUtils.actorActual());
    }

    @PatchMapping("/{id}/repartidor")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Asignar repartidor (ADMINISTRADOR)")
    public BoletaResponse asignarRepartidor(@PathVariable Long id, @Valid @RequestBody AsignarRepartidorRequest req) {
        return boletaService.asignarRepartidor(id, req.repartidorId());
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','REPARTIDOR','CLIENTE')")
    @Operation(summary = "Cambiar estado de la boleta",
            description = "Flujo PENDIENTE -> EN_RUTA -> ENTREGADO; CANCELADO repone el stock. "
                    + "ADMINISTRADOR: cualquier transición válida. REPARTIDOR: solo sus boletas, no cancela. CLIENTE: solo cancelar las suyas pendientes.")
    public BoletaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoBoletaRequest req) {
        return boletaService.cambiarEstado(id, req.estado(), SecurityUtils.actorActual());
    }

    @PostMapping("/{id}/pago-efectivo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','REPARTIDOR')")
    @Operation(summary = "Registrar pago en efectivo (ADMINISTRADOR, REPARTIDOR asignado)")
    public BoletaResponse registrarPagoEfectivo(@PathVariable Long id) {
        return boletaService.registrarPagoEfectivo(id, SecurityUtils.actorActual());
    }
}
