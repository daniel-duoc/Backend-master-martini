
package cl.mastermartini.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void configurar() {

        jwtService = new JwtService(
                System.getenv("JWT_KEYSTORE_PATH"),
                System.getenv("JWT_KEYSTORE_PASSWORD"),
                "martini-jwt",
                3600000,
                "master-martini"
        );
    }

    @Test
    void generarTokenCorrectamente() {

        String token = jwtService.generarToken(
                1L, "prueba@example.com");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void validarTokenCorrecto() {

        String token = jwtService.generarToken(
                1L, "prueba@example.com");

        assertTrue(jwtService.validarToken(token));
    }

    @Test
    void obtenerUsuarioIdCorrectamente() {

        String token = jwtService.generarToken(
                1L, "prueba@example.com");

        assertEquals(1L, jwtService.obtenerUsuarioId(token));
    }

    @Test
    void obtenerCorreoCorrectamente() {

        String token = jwtService.generarToken(
                1L, "prueba@example.com");

        assertEquals("prueba@example.com",
                jwtService.obtenerCorreo(token));
    }

    @Test
    void rechazarTokenInvalido() {

        assertFalse(jwtService.validarToken("token-invalido"));
    }

    @Test
    void rechazarTokenManipulado() {

        String token = jwtService.generarToken(
                1L, "prueba@example.com");

        String[] partes = token.split("\\.");

        String firma = partes[2];

        String firmaModificada =
                (firma.startsWith("A") ? "B" : "A")
                        + firma.substring(1);

        String tokenManipulado = partes[0] + "."
                + partes[1] + "." + firmaModificada;

        assertFalse(jwtService.validarToken(tokenManipulado));
    }

    @Test
    void rechazarTokenVencido() {

        JwtService servicioExpirado = new JwtService(
                System.getenv("JWT_KEYSTORE_PATH"),
                System.getenv("JWT_KEYSTORE_PASSWORD"),
                "martini-jwt",
                -60000,
                "master-martini"
        );

        String token = servicioExpirado.generarToken(
                1L, "prueba@example.com");

        assertFalse(jwtService.validarToken(token));
    }
}

