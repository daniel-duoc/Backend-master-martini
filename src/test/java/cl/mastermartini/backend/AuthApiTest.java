package cl.mastermartini.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
class AuthApiTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .apply(springSecurity())
                .build();
    }

    private String correoNuevo() {
        return "u" + System.nanoTime() + "@martini.cl";
    }

    private String jsonRegistro(String nombre, String correo, String password) {
        return "{\"nombre\":\"" + nombre + "\",\"correo\":\"" + correo
                + "\",\"password\":\"" + password + "\"}";
    }

    private String jsonLogin(String correo, String password) {
        return "{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}";
    }

    private String registrar(String correo) throws Exception {
        return mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegistro("Demo", correo, "Martini1234")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String extraerToken(String respuesta) {
        Matcher m = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(respuesta);
        assertTrue(m.find(), "La respuesta no trae token");
        return m.group(1);
    }

    @Test
    void registroDevuelve201ConToken() throws Exception {
        String respuesta = registrar(correoNuevo());

        assertTrue(respuesta.contains("\"token\""));
    }

    @Test
    void registroRepetidoDevuelve409() throws Exception {
        String correo = correoNuevo();
        registrar(correo);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegistro("Demo", correo, "Martini1234")))
                .andExpect(status().isConflict());
    }

    @Test
    void datosInvalidosDevuelven400() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegistro("", "mal", "123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginCorrectoDevuelve200ConToken() throws Exception {
        String correo = correoNuevo();
        registrar(correo);

        String respuesta = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin(correo, "Martini1234")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(respuesta.contains("\"token\""));
    }

    @Test
    void loginConClaveIncorrectaDevuelve401() throws Exception {
        String correo = correoNuevo();
        registrar(correo);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin(correo, "ClaveEquivocada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutaProtegidaSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutaProtegidaConTokenDevuelve200() throws Exception {
        String correo = correoNuevo();
        String token = extraerToken(registrar(correo));

        String respuesta = mvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(respuesta.contains(correo));
    }

    @Test
    void rutaProtegidaConTokenFalsoDevuelve401() throws Exception {
        mvc.perform(get("/api/me")
                        .header("Authorization", "Bearer token.falso.123"))
                .andExpect(status().isUnauthorized());
    }
}
