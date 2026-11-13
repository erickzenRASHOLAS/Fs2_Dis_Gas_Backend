package cl.duoc.DisGasBackend.controller;

import cl.duoc.DisGasBackend.dto.Dtos.BoletaRequest;
import cl.duoc.DisGasBackend.dto.Dtos.EstadoRequest;
import cl.duoc.DisGasBackend.model.Boleta;
import cl.duoc.DisGasBackend.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CONTROLLER de boletas (pedidos). El rol del usuario sale del token JWT y decide que boletas puede ver. */
@RestController
@RequestMapping("/v1/boletas")
@Tag(name = "Boletas (pedidos)", description = "CLIENTE ve solo las suyas; ADMINISTRADOR y VENDEDOR ven todas")
public class BoletaController {

    private final BoletaService boletaService;

    public BoletaController(BoletaService boletaService) {
        this.boletaService = boletaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un pedido (CLIENTE, ADMINISTRADOR)",
            description = "Envia solo productoId y cantidad. El servidor calcula el total y descuenta el stock. 409 si no hay stock.")
    public Boleta crear(@Valid @RequestBody BoletaRequest req, Authentication authentication) {
        return boletaService.crear(authentication.getName(), req);
    }

    @GetMapping
    @Operation(summary = "Listar boletas", description = "CLIENTE: solo las suyas. ADMINISTRADOR y VENDEDOR: todas.")
    public List<Boleta> listar(Authentication authentication) {
        return boletaService.listar(authentication.getName(), esCliente(authentication));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una boleta", description = "403 si un cliente pide una boleta que no es suya.")
    public Boleta obtener(@PathVariable Long id, Authentication authentication) {
        return boletaService.obtener(id, authentication.getName(), esCliente(authentication));
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Cambiar el estado (ADMINISTRADOR)", description = "Flujo: PENDIENTE a EN_RUTA a ENTREGADO. Cualquier otro salto da 409.")
    public Boleta cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest req) {
        return boletaService.cambiarEstado(id, req.estado());
    }

    @PutMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar una boleta (CLIENTE las suyas pendientes, ADMINISTRADOR)", description = "Devuelve el stock de los productos.")
    public Boleta cancelar(@PathVariable Long id, Authentication authentication) {
        return boletaService.cancelar(id, authentication.getName(), tieneRol(authentication, "ADMINISTRADOR"));
    }

    // ---------- Ayudas para saber el rol del usuario (viene del token JWT) ----------

    private boolean esCliente(Authentication authentication) {
        return tieneRol(authentication, "CLIENTE");
    }

    private boolean tieneRol(Authentication authentication, String rol) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
    }
}
