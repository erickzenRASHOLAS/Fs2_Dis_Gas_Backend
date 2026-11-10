package cl.duoc.DisGasBackend.service;

import cl.duoc.DisGasBackend.dto.Dtos.BoletaRequest;
import cl.duoc.DisGasBackend.dto.Dtos.ItemRequest;
import cl.duoc.DisGasBackend.model.Boleta;
import cl.duoc.DisGasBackend.model.DetalleBoleta;
import cl.duoc.DisGasBackend.model.EstadoBoleta;
import cl.duoc.DisGasBackend.model.Producto;
import cl.duoc.DisGasBackend.model.Usuario;
import cl.duoc.DisGasBackend.repository.BoletaRepository;
import cl.duoc.DisGasBackend.repository.ProductoRepository;
import cl.duoc.DisGasBackend.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * LOGICA DE NEGOCIO de las boletas (pedidos):
 *  1. El servidor calcula precios y total (no se confia en el cliente).
 *  2. Al comprar se valida el stock y se descuenta.
 *  3. Al cancelar se devuelve el stock.
 *  4. El estado sigue el flujo PENDIENTE -> EN_RUTA -> ENTREGADO.
 *  5. Un cliente solo ve y cancela SUS boletas.
 *
 * @Transactional: si algo falla a mitad de camino, se deshacen todos los cambios (ej: no queda stock descontado).
 */
@Service
public class BoletaService {

    private final BoletaRepository boletaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public BoletaService(BoletaRepository boletaRepository, ProductoRepository productoRepository,
                         UsuarioRepository usuarioRepository) {
        this.boletaRepository = boletaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Boleta crear(String correo, BoletaRequest req) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Boleta boleta = new Boleta();
        boleta.setUsuario(usuario);
        boleta.setDireccion(req.direccion());
        boleta.setFecha(LocalDateTime.now());
        boleta.setEstado(EstadoBoleta.PENDIENTE);

        int total = 0;
        for (ItemRequest item : req.items()) {
            Producto producto = productoRepository.findById(item.productoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Producto " + item.productoId() + " no encontrado"));
            if (!producto.isActivo()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El producto " + producto.getNombre() + " no esta disponible");
            }
            if (producto.getStock() < item.cantidad()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stock insuficiente de " + producto.getNombre() + " (disponible: " + producto.getStock() + ")");
            }
            // Descontar stock (Hibernate guarda el cambio al terminar la transaccion)
            producto.setStock(producto.getStock() - item.cantidad());

            DetalleBoleta detalle = new DetalleBoleta();
            detalle.setBoleta(boleta);
            detalle.setProducto(producto);
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioUnitario(producto.getPrecio());               // precio tomado de la BD
            detalle.setSubtotal(producto.getPrecio() * item.cantidad());
            boleta.getDetalles().add(detalle);

            total += detalle.getSubtotal();
        }
        boleta.setTotal(total);
        return boletaRepository.save(boleta);
    }

    /** soloPropias = true para CLIENTE (ve solo las suyas); false para ADMIN y VENDEDOR (ven todas). */
    @Transactional(readOnly = true)
    public List<Boleta> listar(String correo, boolean soloPropias) {
        if (soloPropias) {
            return boletaRepository.findByUsuarioCorreoOrderByFechaDesc(correo);
        }
        return boletaRepository.findAllByOrderByFechaDesc();
    }

    @Transactional(readOnly = true)
    public Boleta obtener(Long id, String correo, boolean soloPropias) {
        Boleta boleta = buscar(id);
        if (soloPropias && !boleta.getUsuario().getCorreo().equals(correo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta boleta no te pertenece");
        }
        return boleta;
    }

    /** Solo ADMIN. Avanza el pedido un paso: PENDIENTE -> EN_RUTA -> ENTREGADO. */
    @Transactional
    public Boleta cambiarEstado(Long id, EstadoBoleta nuevo) {
        Boleta boleta = buscar(id);
        EstadoBoleta actual = boleta.getEstado();
        boolean pasoValido = (actual == EstadoBoleta.PENDIENTE && nuevo == EstadoBoleta.EN_RUTA)
                || (actual == EstadoBoleta.EN_RUTA && nuevo == EstadoBoleta.ENTREGADO);
        if (!pasoValido) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede pasar de " + actual + " a " + nuevo
                            + " (flujo: PENDIENTE, EN_RUTA, ENTREGADO). Para anular usa /cancelar");
        }
        boleta.setEstado(nuevo);
        return boleta;
    }

    /** CLIENTE: cancela sus boletas PENDIENTES. ADMIN: cancela cualquiera que no este entregada. */
    @Transactional
    public Boleta cancelar(Long id, String correo, boolean esAdmin) {
        Boleta boleta = buscar(id);
        if (!esAdmin && !boleta.getUsuario().getCorreo().equals(correo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta boleta no te pertenece");
        }
        EstadoBoleta estado = boleta.getEstado();
        boolean sePuedeCancelar = esAdmin
                ? (estado == EstadoBoleta.PENDIENTE || estado == EstadoBoleta.EN_RUTA)
                : estado == EstadoBoleta.PENDIENTE;
        if (!sePuedeCancelar) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La boleta esta " + estado + " y ya no se puede cancelar");
        }
        // Devolver el stock de cada producto
        for (DetalleBoleta detalle : boleta.getDetalles()) {
            Producto producto = detalle.getProducto();
            producto.setStock(producto.getStock() + detalle.getCantidad());
        }
        boleta.setEstado(EstadoBoleta.CANCELADO);
        return boleta;
    }

    private Boleta buscar(Long id) {
        return boletaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Boleta no encontrada"));
    }
}
