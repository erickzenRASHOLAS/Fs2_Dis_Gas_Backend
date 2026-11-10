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

/**
 * Pruebas de INTEGRACION: se levanta toda la aplicacion (controladores, seguridad JWT, JPA y base H2)
 * y se hacen peticiones HTTP reales para comprobar login, roles y permisos.
 */
@SpringBootTest
class ApiIntegrationTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ---------- ayudas ----------

    private String valorDe(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertThat(m.find()).as("campo " + campo + " en " + json).isTrue();
        return m.group(1);
    }

    private String loginJson(String correo, String password) throws Exception {
        String cuerpo = "{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}";
        MvcResult r = mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk()).andReturn();
        return r.getResponse().getContentAsString();
    }

    private String token(String correo, String password) throws Exception {
        return valorDe(loginJson(correo, password), "token");
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder peticion, String token) {
        return peticion.header("Authorization", "Bearer " + token);
    }

    // ---------- autenticacion ----------

    @Test
    void login_correcto_entregaTokensYNoExponeLaPassword() throws Exception {
        String respuesta = loginJson("admin@volcan.cl", "admin123");
        assertThat(respuesta).contains("\"token\"", "\"refreshToken\"", "ADMINISTRADOR");
        assertThat(respuesta).doesNotContain("admin123");
    }

    @Test
    void login_conClaveIncorrecta_devuelve401() throws Exception {
        mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"admin@volcan.cl\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registro_creaUnClienteYRechazaElCorreoDuplicado() throws Exception {
        String correo = "nuevo-" + UUID.randomUUID() + "@test.cl";
        String cuerpo = "{\"nombre\":\"Nuevo\",\"correo\":\"" + correo + "\",\"telefono\":\"123\","
                + "\"direccion\":\"Calle 1\",\"password\":\"secreto1\"}";

        MvcResult r = mvc.perform(post("/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andReturn();
        assertThat(r.getResponse().getContentAsString()).contains("CLIENTE");

        mvc.perform(post("/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
    }

    @Test
    void refresh_conRefreshToken_entregaTokenNuevo_yConAccessToken_devuelve401() throws Exception {
        String login = loginJson("juan.perez@email.com", "123456");
        String refresh = valorDe(login, "refreshToken");
        String access = valorDe(login, "token");

        MvcResult r = mvc.perform(post("/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getContentAsString()).contains("\"token\"");

        mvc.perform(post("/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + access + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- roles y permisos ----------

    @Test
    void sinToken_lasRutasProtegidasDevuelven401() throws Exception {
        mvc.perform(get("/v1/boletas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAlterado_devuelve401() throws Exception {
        String t = token("admin@volcan.cl", "admin123");
        mvc.perform(conToken(get("/v1/usuarios"), t + "x")).andExpect(status().isUnauthorized());
    }

    @Test
    void elCatalogoEsPublico() throws Exception {
        MvcResult r = mvc.perform(get("/v1/productos")).andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getContentAsString()).contains("Cilindro 5 kg");
        mvc.perform(get("/v1/categorias")).andExpect(status().isOk());
    }

    @Test
    void soloElAdministradorVeLosUsuarios() throws Exception {
        mvc.perform(conToken(get("/v1/usuarios"), token("admin@volcan.cl", "admin123"))).andExpect(status().isOk());
        mvc.perform(conToken(get("/v1/usuarios"), token("juan.perez@email.com", "123456"))).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/v1/usuarios"), token("ana.vendedora@volcan.cl", "123456"))).andExpect(status().isForbidden());
    }

    @Test
    void vendedor_veProductosYBoletas_peroNoPuedeCrearProductos() throws Exception {
        String t = token("ana.vendedora@volcan.cl", "123456");
        mvc.perform(conToken(get("/v1/productos/todos"), t)).andExpect(status().isOk());
        mvc.perform(conToken(get("/v1/boletas"), t)).andExpect(status().isOk());
        mvc.perform(conToken(post("/v1/productos"), t).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cliente_noPuedeEntrarAZonasDeAdministracion() throws Exception {
        String t = token("juan.perez@email.com", "123456");
        mvc.perform(conToken(post("/v1/productos"), t).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(conToken(get("/v1/productos/todos"), t)).andExpect(status().isForbidden());
    }

    // ---------- flujo de compra ----------

    @Test
    void cliente_creaUnPedido_yElServidorCalculaElTotal() throws Exception {
        String t = token("juan.perez@email.com", "123456");
        // productoId 1 = Cilindro 5 kg ($10.800). Si el cliente enviara "total", se ignora.
        String pedido = "{\"direccion\":\"Av. Brasil 456\",\"total\":1,\"items\":[{\"productoId\":1,\"cantidad\":2}]}";

        MvcResult r = mvc.perform(conToken(post("/v1/boletas"), t).contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isCreated()).andReturn();
        assertThat(r.getResponse().getContentAsString()).contains("\"total\":21600", "PENDIENTE");

        MvcResult lista = mvc.perform(conToken(get("/v1/boletas"), t)).andExpect(status().isOk()).andReturn();
        assertThat(lista.getResponse().getContentAsString()).contains("\"total\":21600");
    }

    @Test
    void cliente_pidiendoMasDelStockDisponible_recibe409() throws Exception {
        String t = token("juan.perez@email.com", "123456");
        String pedido = "{\"direccion\":\"Calle 1\",\"items\":[{\"productoId\":1,\"cantidad\":9999}]}";
        mvc.perform(conToken(post("/v1/boletas"), t).contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isConflict());
    }

    @Test
    void datosInvalidos_devuelven400() throws Exception {
        mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
