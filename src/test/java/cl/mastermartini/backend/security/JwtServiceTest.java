package cl.mastermartini.backend.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRETO = "clave-de-prueba-0123456789-abcdefghijklmnop";
    private static final String OTRO_SECRETO = "otra-clave-distinta-0123456789-abcdefghijklmnop";

    @Test
    void generaTokenValidoYExtraeElCorreo() {
        JwtService servicio = new JwtService(SECRETO, 60000);

        String token = servicio.generarToken("demo@martini.cl");

        assertTrue(servicio.esValido(token));
        assertEquals("demo@martini.cl", servicio.extraerCorreo(token));
    }

    @Test
    void tokenConFirmaFalsaNoEsValido() {
        JwtService servicio = new JwtService(SECRETO, 60000);
        String[] partes = servicio.generarToken("demo@martini.cl").split("\\.");

        String manipulado = partes[0] + "." + partes[1] + ".firmaFalsa";

        assertFalse(servicio.esValido(manipulado));
    }

    @Test
    void tokenExpiradoNoEsValido() {
        JwtService servicio = new JwtService(SECRETO, -1000);

        String token = servicio.generarToken("demo@martini.cl");

        assertFalse(servicio.esValido(token));
    }

    @Test
    void tokenFirmadoConOtraClaveNoEsValido() {
        JwtService emisor = new JwtService(OTRO_SECRETO, 60000);
        JwtService validador = new JwtService(SECRETO, 60000);

        String token = emisor.generarToken("demo@martini.cl");

        assertFalse(validador.esValido(token));
    }

    @Test
    void textoQueNoEsTokenNoEsValido() {
        JwtService servicio = new JwtService(SECRETO, 60000);

        assertFalse(servicio.esValido("esto-no-es-un-token"));
    }
}

