package cl.duoc.DisGasBackend.service;

import cl.duoc.DisGasBackend.dto.BoletaRequest;
import cl.duoc.DisGasBackend.dto.BoletaResponse;
import cl.duoc.DisGasBackend.dto.ItemBoletaRequest;
import cl.duoc.DisGasBackend.exception.ConflictoException;
import cl.duoc.DisGasBackend.exception.RecursoNoEncontradoException;
import cl.duoc.DisGasBackend.exception.SolicitudInvalidaException;
import cl.duoc.DisGasBackend.model.Boleta;
import cl.duoc.DisGasBackend.model.DetalleBoleta;
import cl.duoc.DisGasBackend.model.Estado;
import cl.duoc.DisGasBackend.model.EstadoBoleta;
import cl.duoc.DisGasBackend.model.EstadoPago;
import cl.duoc.DisGasBackend.model.Producto;
import cl.duoc.DisGasBackend.model.Rol;
import cl.duoc.DisGasBackend.model.Usuario;
import cl.duoc.DisGasBackend.repository.BoletaRepository;
import cl.duoc.DisGasBackend.repository.ProductoRepository;
import cl.duoc.DisGasBackend.repository.UsuarioRepository;
import cl.duoc.DisGasBackend.security.Actor;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio de las boletas (pedidos):
 * - el servidor calcula precios y total (no se confía en el cliente),
 * - el stock se descuenta al comprar y se repone al cancelar,
 * - el estado sigue un flujo PENDIENTE -> EN_RUTA -> ENTREGADO (o CANCELADO),
 * - cada rol solo ve y modifica lo que le corresponde.
 */
@Service
@RequiredArgsConstructor
public class BoletaService {

    private static final Map<EstadoBoleta, Set<EstadoBoleta>> TRANSICIONES = Map.of(
            EstadoBoleta.PENDIENTE, EnumSet.of(EstadoBoleta.EN_RUTA, EstadoBoleta.CANCELADO),
            EstadoBoleta.EN_RUTA, EnumSet.of(EstadoBoleta.ENTREGADO, EstadoBoleta.CANCELADO),
            EstadoBoleta.ENTREGADO, EnumSet.noneOf(EstadoBoleta.class),
            EstadoBoleta.CANCELADO, EnumSet.noneOf(EstadoBoleta.class));

    private final BoletaRepository boletaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public BoletaResponse crear(Actor actor, BoletaRequest req) {
        Usuario usuario = usuarioRepository.findById(actor.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        // Si el mismo producto viene repetido, se suman las cantidades.
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (ItemBoletaRequest item : req.items()) {
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }

        Map<Long, Producto> productos = productoRepository.findAllByIdForUpdate(cantidades.keySet()).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));

        Boleta boleta = new Boleta();
        boleta.setUsuario(usuario);
        boleta.setNombreCliente(req.nombreCliente().trim());
        boleta.setTelefono(req.telefono().trim());
        boleta.setDireccion(req.direccion().trim());
        boleta.setFecha(LocalDateTime.now());

        int total = 0;
        for (Map.Entry<Long, Integer> e : cantidades.entrySet()) {
            Producto p = productos.get(e.getKey());
            if (p == null || p.getEstado() != Estado.ACTIVO) {
                throw new SolicitudInvalidaException("El producto con id " + e.getKey() + " no existe o no está disponible");
            }
            int cantidad = e.getValue();
            if (p.getStock() < cantidad) {
                throw new ConflictoException("Stock insuficiente para '" + p.getNombre() + "'. Disponible: " + p.getStock());
            }
            p.setStock(p.getStock() - cantidad);

            DetalleBoleta detalle = new DetalleBoleta();
            detalle.setProducto(p);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(p.getPrecio());
            detalle.setSubtotal(p.getPrecio() * cantidad);
            boleta.agregarDetalle(detalle);
            total += detalle.getSubtotal();
        }
        boleta.setTotal(total);
        return BoletaResponse.from(boletaRepository.save(boleta));
    }

    @Transactional(readOnly = true)
    public List<BoletaResponse> listar(Actor actor, EstadoBoleta estado) {
        List<Boleta> boletas = switch (actor.rol()) {
            case ADMINISTRADOR, VENDEDOR -> boletaRepository.findAllByOrderByFechaDescIdDesc();
            case CLIENTE -> boletaRepository.findByUsuarioIdOrderByFechaDescIdDesc(actor.id());
            case REPARTIDOR -> boletaRepository.findByRepartidorIdOrderByFechaDescIdDesc(actor.id());
        };
        return boletas.stream()
                .filter(b -> estado == null || b.getEstado() == estado)
                .map(BoletaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BoletaResponse obtener(Long id, Actor actor) {
        Boleta b = buscar(id);
        verificarAcceso(b, actor);
        return BoletaResponse.from(b);
    }

    @Transactional
    public BoletaResponse asignarRepartidor(Long id, Long repartidorId) {
        Boleta b = buscar(id);
        if (b.getEstado() == EstadoBoleta.ENTREGADO || b.getEstado() == EstadoBoleta.CANCELADO) {
            throw new ConflictoException("No se puede asignar repartidor a una boleta " + b.getEstado());
        }
        Usuario repartidor = usuarioRepository.findById(repartidorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Repartidor no encontrado (id " + repartidorId + ")"));
        if (repartidor.getRol() != Rol.REPARTIDOR || repartidor.getEstado() != Estado.ACTIVO) {
            throw new SolicitudInvalidaException("El usuario indicado no es un repartidor activo");
        }
        b.setRepartidor(repartidor);
        return BoletaResponse.from(boletaRepository.save(b));
    }

    @Transactional
    public BoletaResponse cambiarEstado(Long id, EstadoBoleta nuevo, Actor actor) {
        Boleta b = buscar(id);
        verificarAcceso(b, actor);

        if (!TRANSICIONES.get(b.getEstado()).contains(nuevo)) {
            throw new ConflictoException("No se puede cambiar el estado de " + b.getEstado() + " a " + nuevo);
        }
        // Permisos por rol sobre el cambio de estado.
        switch (actor.rol()) {
            case ADMINISTRADOR -> { /* puede ejecutar cualquier transición válida */ }
            case REPARTIDOR -> {
                if (nuevo == EstadoBoleta.CANCELADO) {
                    throw new AccessDeniedException("El repartidor no puede cancelar boletas");
                }
            }
            case CLIENTE -> {
                if (nuevo != EstadoBoleta.CANCELADO) {
                    throw new AccessDeniedException("Solo puedes cancelar tus pedidos");
                }
                if (b.getEstado() != EstadoBoleta.PENDIENTE) {
                    throw new ConflictoException("Solo se pueden cancelar pedidos pendientes");
                }
            }
            default -> throw new AccessDeniedException("Tu rol no puede modificar boletas");
        }

        if (nuevo == EstadoBoleta.EN_RUTA && b.getRepartidor() == null) {
            throw new ConflictoException("Debes asignar un repartidor antes de enviar el pedido a ruta");
        }
        if (nuevo == EstadoBoleta.CANCELADO) {
            if (b.getEstadoPago() == EstadoPago.PAGADO) {
                throw new ConflictoException("No se puede cancelar una boleta que ya tiene el pago registrado");
            }
            reponerStock(b);
        }
        b.setEstado(nuevo);
        return BoletaResponse.from(boletaRepository.save(b));
    }

    @Transactional
    public BoletaResponse registrarPagoEfectivo(Long id, Actor actor) {
        Boleta b = buscar(id);
        boolean esRepartidorAsignado = actor.rol() == Rol.REPARTIDOR
                && b.getRepartidor() != null && b.getRepartidor().getId().equals(actor.id());
        if (actor.rol() != Rol.ADMINISTRADOR && !esRepartidorAsignado) {
            throw new AccessDeniedException("No puedes registrar el pago de esta boleta");
        }
        if (b.getEstado() == EstadoBoleta.CANCELADO) {
            throw new ConflictoException("No se puede registrar pago en una boleta cancelada");
        }
        if (b.getEstadoPago() == EstadoPago.PAGADO) {
            return BoletaResponse.from(b); // idempotente: ya estaba pagada
        }
        b.setEstadoPago(EstadoPago.PAGADO);
        b.setMetodoPago("EFECTIVO");
        b.setMontoPagado(b.getTotal());
        b.setFechaPago(LocalDateTime.now());
        return BoletaResponse.from(boletaRepository.save(b));
    }

    private void reponerStock(Boleta b) {
        List<Long> ids = b.getDetalles().stream().map(d -> d.getProducto().getId()).toList();
        Map<Long, Producto> productos = productoRepository.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));
        for (DetalleBoleta d : b.getDetalles()) {
            Producto p = productos.get(d.getProducto().getId());
            if (p != null) {
                p.setStock(p.getStock() + d.getCantidad());
            }
        }
    }

    private void verificarAcceso(Boleta b, Actor actor) {
        boolean permitido = switch (actor.rol()) {
            case ADMINISTRADOR, VENDEDOR -> true;
            case CLIENTE -> b.getUsuario().getId().equals(actor.id());
            case REPARTIDOR -> b.getRepartidor() != null && b.getRepartidor().getId().equals(actor.id());
        };
        if (!permitido) {
            throw new AccessDeniedException("No tienes acceso a esta boleta");
        }
    }

    private Boleta buscar(Long id) {
        return boletaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Boleta no encontrada (id " + id + ")"));
    }
}
