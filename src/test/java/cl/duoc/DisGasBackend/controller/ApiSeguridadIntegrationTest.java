package cl.duoc.DisGasBackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Prueba de integración: API completa + Spring Security + JWT + base H2 con las migraciones Flyway. */
@SpringBootTest
class ApiSeguridadIntegrationTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ---------- helpers ----------

    private String json(String... paresClaveValor) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < paresClaveValor.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(paresClaveValor[i]).append("\":\"").append(paresClaveValor[i + 1]).append('"');
        }
        return sb.append('}').toString();
    }

    private String campo(String cuerpo, String nombre) {
        Matcher m = Pattern.compile("\"" + nombre + "\"\\s*:\\s*\"([^\"]+)\"").matcher(cuerpo);
        assertThat(m.find()).as("campo %s en %s", nombre, cuerpo).isTrue();
        return m.group(1);
    }

    private String loginCompleto(String correo, String password) throws Exception {
        MvcResult r = mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("correo", correo, "password", password)))
                .andExpect(status().isOk()).andReturn();
        return r.getResponse().getContentAsString();
    }

    private String token(String correo, String password) throws Exception {
        return campo(loginCompleto(correo, password), "accessToken");
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    // ---------- autenticación ----------

    @Test
    void loginConCredencialesCorrectas_entregaTokens() throws Exception {
        String cuerpo = loginCompleto("admin@volcan.cl", "admin123");
        assertThat(cuerpo).contains("\"accessToken\"", "\"refreshToken\"", "ADMINISTRADOR");
        assertThat(cuerpo).doesNotContain("password");
    }

    @Test
    void loginConClaveIncorrecta_devuelve401() throws Exception {
        mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("correo", "admin@volcan.cl", "password", "incorrecta")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginConDatosVacios_devuelve400() throws Exception {
        mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("correo", "", "password", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registro_creaClienteYRechazaCorreoDuplicado() throws Exception {
        String correo = "nuevo-" + UUID.randomUUID() + "@test.cl";
        String body = json("nombre", "Nuevo Cliente", "correo", correo, "telefono", "+56911112222",
                "direccion", "Los Pinos 123", "password", "secreto1");

        MvcResult r = mvc.perform(post("/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        assertThat(r.getResponse().getContentAsString()).contains("CLIENTE").doesNotContain("secreto1");

        mvc.perform(post("/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void registro_conContrasenaCorta_devuelve400() throws Exception {
        String body = json("nombre", "X", "correo", "x-" + UUID.randomUUID() + "@test.cl", "telefono", "1",
                "direccion", "Calle", "password", "123");
        mvc.perform(post("/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ---------- renovación de tokens ----------

    @Test
    void refresh_conTokenValido_entregaNuevoPar() throws Exception {
        String refresh = campo(loginCompleto("juan.perez@email.com", "123456"), "refreshToken");
        MvcResult r = mvc.perform(post("/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json("refreshToken", refresh)))
                .andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getContentAsString()).contains("\"accessToken\"");
    }

    @Test
    void refresh_conAccessToken_devuelve401() throws Exception {
        String access = token("juan.perez@email.com", "123456");
        mvc.perform(post("/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json("refreshToken", access)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenNoSirveComoAccessToken() throws Exception {
        String refresh = campo(loginCompleto("juan.perez@email.com", "123456"), "refreshToken");
        mvc.perform(conToken(get("/v1/boletas"), refresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenManipulado_devuelve401() throws Exception {
        String t = token("admin@volcan.cl", "admin123");
        mvc.perform(conToken(get("/v1/usuarios"), t + "x")).andExpect(status().isUnauthorized());
    }

    // ---------- roles y permisos ----------

    @Test
    void catalogoEsPublico() throws Exception {
        MvcResult r = mvc.perform(get("/v1/productos")).andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getContentAsString()).contains("Cilindro 5 kg");
        mvc.perform(get("/v1/categorias")).andExpect(status().isOk());
    }

    @Test
    void sinToken_rutasPrivadasDevuelven401() throws Exception {
        mvc.perform(get("/v1/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/boletas")).andExpect(status().isUnauthorized());
    }

    @Test
    void administrador_puedeListarUsuarios_perosoloEl() throws Exception {
        mvc.perform(conToken(get("/v1/usuarios"), token("admin@volcan.cl", "admin123"))).andExpect(status().isOk());
        mvc.perform(conToken(get("/v1/usuarios"), token("juan.perez@email.com", "123456"))).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/v1/usuarios"), token("ana.vendedora@volcan.cl", "123456"))).andExpect(status().isForbidden());
    }

    @Test
    void vendedor_veProductosYBoletas_peroNoModifica() throws Exception {
        String t = token("ana.vendedora@volcan.cl", "123456");
        mvc.perform(conToken(get("/v1/productos"), t)).andExpect(status().isOk());
        mvc.perform(conToken(get("/v1/boletas"), t)).andExpect(status().isOk());

        String producto = json("codigo", "x-1", "nombre", "X", "descripcion", "d", "precio", "1", "stock", "1");
        mvc.perform(conToken(post("/v1/productos"), t).contentType(MediaType.APPLICATION_JSON).content(producto))
                .andExpect(status().isForbidden());

        String pedido = "{\"nombreCliente\":\"A\",\"telefono\":\"1\",\"direccion\":\"B\",\"items\":[{\"productoId\":1,\"cantidad\":1}]}";
        mvc.perform(conToken(post("/v1/boletas"), t).contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isForbidden());
    }

    @Test
    void cliente_creaPedido_conTotalCalculadoPorElServidor() throws Exception {
        String t = token("juan.perez@email.com", "123456");
        // productoId 1 = Cilindro 5 kg ($10.800). Si el cliente intentara enviar un precio, se ignora.
        String pedido = "{\"nombreCliente\":\"Juan\",\"telefono\":\"+56912345678\",\"direccion\":\"Av. Brasil 456\","
                + "\"total\":1,\"items\":[{\"productoId\":1,\"cantidad\":2}]}";
        MvcResult r = mvc.perform(conToken(post("/v1/boletas"), t).contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isCreated()).andReturn();
        assertThat(r.getResponse().getContentAsString()).contains("\"total\":21600", "PENDIENTE");

        // y el cliente ve su pedido en su lista
        MvcResult lista = mvc.perform(conToken(get("/v1/boletas"), t)).andExpect(status().isOk()).andReturn();
        assertThat(lista.getResponse().getContentAsString()).contains("\"total\":21600");
    }

    @Test
    void cliente_conStockInsuficiente_recibe409() throws Exception {
        String t = token("juan.perez@email.com", "123456");
        String pedido = "{\"nombreCliente\":\"Juan\",\"telefono\":\"1\",\"direccion\":\"B\",\"items\":[{\"productoId\":1,\"cantidad\":100}]}";
        // 100 unidades superan el stock inicial (32) sin exceder el máximo por ítem
        mvc.perform(conToken(post("/v1/boletas"), t).contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isConflict());
    }

    @Test
    void cliente_noPuedeCrearProductos() throws Exception {
        String t = token("juan.perez@email.com", "123456");
        mvc.perform(conToken(post("/v1/productos"), t).contentType(MediaType.APPLICATION_JSON)
                .content(json("codigo", "x-2", "nombre", "X"))).andExpect(status().isForbidden());
    }

    @Test
    void jsonMalFormado_devuelve400_noError500() throws Exception {
        mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{correo: sin comillas"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void swaggerYHealthSonPublicos() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
