package cl.duoc.DisGasBackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cl.duoc.DisGasBackend.dto.BoletaRequest;
import cl.duoc.DisGasBackend.dto.BoletaResponse;
import cl.duoc.DisGasBackend.dto.ItemBoletaRequest;
import cl.duoc.DisGasBackend.exception.ConflictoException;
import cl.duoc.DisGasBackend.model.Boleta;
import cl.duoc.DisGasBackend.model.Categoria;
import cl.duoc.DisGasBackend.model.DetalleBoleta;
import cl.duoc.DisGasBackend.model.Estado;
import cl.duoc.DisGasBackend.model.EstadoBoleta;
import cl.duoc.DisGasBackend.model.Producto;
import cl.duoc.DisGasBackend.model.Rol;
import cl.duoc.DisGasBackend.model.Usuario;
import cl.duoc.DisGasBackend.repository.BoletaRepository;
import cl.duoc.DisGasBackend.repository.ProductoRepository;
import cl.duoc.DisGasBackend.repository.UsuarioRepository;
import cl.duoc.DisGasBackend.security.Actor;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class BoletaServiceTest {

    @Mock
    BoletaRepository boletaRepository;
    @Mock
    ProductoRepository productoRepository;
    @Mock
    UsuarioRepository usuarioRepository;

    @InjectMocks
    BoletaService service;

    private Usuario usuario(Long id, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Usuario " + id);
        u.setCorreo("u" + id + "@test.cl");
        u.setRol(rol);
        return u;
    }

    private Producto producto(Long id, int precio, int stock) {
        Categoria c = new Categoria();
        c.setId(1L);
        c.setNombre("Gas");
        Producto p = new Producto();
        p.setId(id);
        p.setCodigo("gas-" + id);
        p.setNombre("Cilindro " + id);
        p.setPrecio(precio);
        p.setStock(stock);
        p.setEstado(Estado.ACTIVO);
        p.setCategoria(c);
        return p;
    }

    private Boleta boleta(Long id, Usuario cliente, Producto producto, int cantidad, EstadoBoleta estado) {
        Boleta b = new Boleta();
        b.setId(id);
        b.setUsuario(cliente);
        b.setNombreCliente("Cliente");
        b.setTelefono("123");
        b.setDireccion("Calle 1");
        b.setEstado(estado);
        DetalleBoleta d = new DetalleBoleta();
        d.setProducto(producto);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(producto.getPrecio());
        d.setSubtotal(producto.getPrecio() * cantidad);
        b.agregarDetalle(d);
        b.setTotal(d.getSubtotal());
        return b;
    }

    @Test
    void crearBoleta_calculaTotalEnElServidorYDescuentaStock() {
        Usuario cliente = usuario(1L, Rol.CLIENTE);
        Producto p = producto(10L, 21500, 5);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(productoRepository.findAllByIdForUpdate(any())).thenReturn(List.of(p));
        when(boletaRepository.save(any(Boleta.class))).thenAnswer(inv -> {
            Boleta guardada = inv.getArgument(0);
            guardada.setId(100L);
            return guardada;
        });

        // El mismo producto repetido se consolida: 1 + 2 = 3 unidades
        BoletaRequest req = new BoletaRequest("Juan", "+56911112222", "Av. Brasil 456",
                List.of(new ItemBoletaRequest(10L, 1), new ItemBoletaRequest(10L, 2)));

        BoletaResponse resp = service.crear(new Actor(1L, Rol.CLIENTE), req);

        assertThat(resp.total()).isEqualTo(64500);
        assertThat(resp.estado()).isEqualTo(EstadoBoleta.PENDIENTE);
        assertThat(resp.detalles()).hasSize(1);
        assertThat(p.getStock()).isEqualTo(2);
    }

    @Test
    void crearBoleta_conStockInsuficiente_lanzaConflicto() {
        Usuario cliente = usuario(1L, Rol.CLIENTE);
        Producto p = producto(10L, 21500, 1);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(productoRepository.findAllByIdForUpdate(any())).thenReturn(List.of(p));

        BoletaRequest req = new BoletaRequest("Juan", "+56911112222", "Av. Brasil 456",
                List.of(new ItemBoletaRequest(10L, 3)));

        assertThatThrownBy(() -> service.crear(new Actor(1L, Rol.CLIENTE), req))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Stock insuficiente");
        assertThat(p.getStock()).isEqualTo(1);
    }

    @Test
    void clienteNoPuedeVerBoletaDeOtroCliente() {
        Boleta ajena = boleta(7L, usuario(2L, Rol.CLIENTE), producto(10L, 1000, 5), 1, EstadoBoleta.PENDIENTE);
        when(boletaRepository.findById(7L)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> service.obtener(7L, new Actor(1L, Rol.CLIENTE)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void vendedorPuedeVerCualquierBoleta() {
        Boleta b = boleta(7L, usuario(2L, Rol.CLIENTE), producto(10L, 1000, 5), 1, EstadoBoleta.PENDIENTE);
        when(boletaRepository.findById(7L)).thenReturn(Optional.of(b));

        assertThat(service.obtener(7L, new Actor(99L, Rol.VENDEDOR)).id()).isEqualTo(7L);
    }

    @Test
    void vendedorNoPuedeCambiarEstado() {
        Boleta b = boleta(7L, usuario(2L, Rol.CLIENTE), producto(10L, 1000, 5), 1, EstadoBoleta.PENDIENTE);
        when(boletaRepository.findById(7L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.cambiarEstado(7L, EstadoBoleta.CANCELADO, new Actor(99L, Rol.VENDEDOR)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cancelarBoleta_reponeElStock() {
        Producto p = producto(10L, 1000, 3);
        Boleta b = boleta(7L, usuario(2L, Rol.CLIENTE), p, 2, EstadoBoleta.PENDIENTE);
        when(boletaRepository.findById(7L)).thenReturn(Optional.of(b));
        when(productoRepository.findAllByIdForUpdate(any())).thenReturn(List.of(p));
        when(boletaRepository.save(any(Boleta.class))).thenAnswer(inv -> inv.getArgument(0));

        BoletaResponse resp = service.cambiarEstado(7L, EstadoBoleta.CANCELADO, new Actor(2L, Rol.CLIENTE));

        assertThat(resp.estado()).isEqualTo(EstadoBoleta.CANCELADO);
        assertThat(p.getStock()).isEqualTo(5);
    }

    @Test
    void enviarARutaSinRepartidor_lanzaConflicto() {
        Boleta b = boleta(7L, usuario(2L, Rol.CLIENTE), producto(10L, 1000, 5), 1, EstadoBoleta.PENDIENTE);
        when(boletaRepository.findById(7L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.cambiarEstado(7L, EstadoBoleta.EN_RUTA, new Actor(1L, Rol.ADMINISTRADOR)))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("repartidor");
    }

    @Test
    void transicionInvalida_lanzaConflicto() {
        Boleta b = boleta(7L, usuario(2L, Rol.CLIENTE), producto(10L, 1000, 5), 1, EstadoBoleta.ENTREGADO);
        when(boletaRepository.findById(7L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.cambiarEstado(7L, EstadoBoleta.PENDIENTE, new Actor(1L, Rol.ADMINISTRADOR)))
                .isInstanceOf(ConflictoException.class);
    }
}
