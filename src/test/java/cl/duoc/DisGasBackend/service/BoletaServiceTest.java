package cl.duoc.DisGasBackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cl.duoc.DisGasBackend.dto.Dtos.BoletaRequest;
import cl.duoc.DisGasBackend.dto.Dtos.ItemRequest;
import cl.duoc.DisGasBackend.model.Boleta;
import cl.duoc.DisGasBackend.model.Categoria;
import cl.duoc.DisGasBackend.model.DetalleBoleta;
import cl.duoc.DisGasBackend.model.EstadoBoleta;
import cl.duoc.DisGasBackend.model.Producto;
import cl.duoc.DisGasBackend.model.Rol;
import cl.duoc.DisGasBackend.model.Usuario;
import cl.duoc.DisGasBackend.repository.BoletaRepository;
import cl.duoc.DisGasBackend.repository.ProductoRepository;
import cl.duoc.DisGasBackend.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Pruebas UNITARIAS de la logica de negocio de boletas (sin base de datos: los repositorios son simulados). */
@ExtendWith(MockitoExtension.class)
class BoletaServiceTest {

    @Mock
    BoletaRepository boletaRepository;
    @Mock
    ProductoRepository productoRepository;
    @Mock
    UsuarioRepository usuarioRepository;

    @InjectMocks
    BoletaService boletaService;

    // ---------- datos de prueba ----------

    private Usuario cliente(Long id, String correo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setCorreo(correo);
        u.setRol(Rol.CLIENTE);
        return u;
    }

    private Producto producto(Long id, int precio, int stock) {
        Categoria c = new Categoria();
        c.setId(1L);
        c.setNombre("Gas");
        Producto p = new Producto();
        p.setId(id);
        p.setNombre("Cilindro " + id);
        p.setPrecio(precio);
        p.setStock(stock);
        p.setCategoria(c);
        return p;
    }

    private Boleta boleta(Usuario dueno, Producto producto, int cantidad, EstadoBoleta estado) {
        Boleta b = new Boleta();
        b.setId(7L);
        b.setUsuario(dueno);
        b.setEstado(estado);
        DetalleBoleta d = new DetalleBoleta();
        d.setBoleta(b);
        d.setProducto(producto);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(producto.getPrecio());
        d.setSubtotal(producto.getPrecio() * cantidad);
        b.getDetalles().add(d);
        b.setTotal(d.getSubtotal());
        return b;
    }

    // ---------- pruebas ----------

    @Test
    void crearBoleta_calculaElTotalYDescuentaElStock() {
        Producto gas = producto(10L, 21500, 5);
        when(usuarioRepository.findByCorreo("juan@test.cl")).thenReturn(Optional.of(cliente(1L, "juan@test.cl")));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(gas));
        when(boletaRepository.save(any(Boleta.class))).thenAnswer(inv -> inv.getArgument(0));

        BoletaRequest pedido = new BoletaRequest("Av. Brasil 456", List.of(new ItemRequest(10L, 2)));
        Boleta resultado = boletaService.crear("juan@test.cl", pedido);

        assertThat(resultado.getTotal()).isEqualTo(43000);          // 2 x 21.500
        assertThat(resultado.getEstado()).isEqualTo(EstadoBoleta.PENDIENTE);
        assertThat(gas.getStock()).isEqualTo(3);                    // 5 - 2
    }

    @Test
    void crearBoleta_sinStockSuficiente_lanzaConflicto() {
        Producto gas = producto(10L, 21500, 1);
        when(usuarioRepository.findByCorreo("juan@test.cl")).thenReturn(Optional.of(cliente(1L, "juan@test.cl")));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(gas));

        BoletaRequest pedido = new BoletaRequest("Av. Brasil 456", List.of(new ItemRequest(10L, 3)));

        assertThatThrownBy(() -> boletaService.crear("juan@test.cl", pedido))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Stock insuficiente");
        assertThat(gas.getStock()).isEqualTo(1);                    // no se descuenta nada
    }

    @Test
    void clienteNoPuedeVerLaBoletaDeOtroCliente() {
        Boleta ajena = boleta(cliente(2L, "maria@test.cl"), producto(10L, 1000, 5), 1, EstadoBoleta.PENDIENTE);
        when(boletaRepository.findById(7L)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> boletaService.obtener(7L, "juan@test.cl", true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void cancelarBoleta_devuelveElStock() {
        Producto gas = producto(10L, 1000, 3);
        Boleta b = boleta(cliente(1L, "juan@test.cl"), gas, 2, EstadoBoleta.PENDIENTE);
        when(boletaRepository.findById(7L)).thenReturn(Optional.of(b));

        Boleta resultado = boletaService.cancelar(7L, "juan@test.cl", false);

        assertThat(resultado.getEstado()).isEqualTo(EstadoBoleta.CANCELADO);
        assertThat(gas.getStock()).isEqualTo(5);                    // 3 + 2 devueltos
    }

    @Test
    void cambiarEstado_siguiendoElFlujoFunciona() {
        Boleta b = boleta(cliente(1L, "juan@test.cl"), producto(10L, 1000, 5), 1, EstadoBoleta.PENDIENTE);
        when(boletaRepository.findById(7L)).thenReturn(Optional.of(b));

        assertThat(boletaService.cambiarEstado(7L, EstadoBoleta.EN_RUTA).getEstado()).isEqualTo(EstadoBoleta.EN_RUTA);
        assertThat(boletaService.cambiarEstado(7L, EstadoBoleta.ENTREGADO).getEstado()).isEqualTo(EstadoBoleta.ENTREGADO);
    }

    @Test
    void cambiarEstado_saltandoUnPaso_lanzaConflicto() {
        Boleta b = boleta(cliente(1L, "juan@test.cl"), producto(10L, 1000, 5), 1, EstadoBoleta.PENDIENTE);
        when(boletaRepository.findById(7L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> boletaService.cambiarEstado(7L, EstadoBoleta.ENTREGADO))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No se puede pasar");
    }
}
