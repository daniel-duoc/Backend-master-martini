package cl.mastermartini.auth.security;

import io.jsonwebtoken.Jwts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private PrivateKey privateKey;

    private static final String EMISOR = "master-martini";
    private static final String CORREO = "prueba@example.com";
    private static final long DURACION = 3600000;

    @BeforeEach
    void configurar() throws Exception {

        String ruta = System.getenv("JWT_KEYSTORE_PATH");
        String password = System.getenv("JWT_KEYSTORE_PASSWORD");

        assertNotNull(ruta);
        assertNotNull(password);

        jwtService = new JwtService(
                ruta,
                password,
                "martini-jwt",
                DURACION,
                EMISOR
        );

        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (InputStream input =
                     Files.newInputStream(Path.of(ruta))) {

            keyStore.load(input, password.toCharArray());
        }

        privateKey = (PrivateKey) keyStore.getKey(
                "martini-jwt",
                password.toCharArray()
        );

        assertNotNull(privateKey);
    }

    // Generar tokens personalizados para las pruebas
    private String generarToken(
            String id,
            String correo,
            String emisor,
            Long duracion
    ) {

        long ahora = System.currentTimeMillis();

        var builder = Jwts.builder()
                .issuer(emisor)
                .issuedAt(new Date(ahora));

        if (id != null) {
            builder.subject(id);
        }

        if (correo != null) {
            builder.claim("correo", correo);
        }

        if (duracion != null) {
            builder.expiration(
                    new Date(ahora + duracion)
            );
        }

        return builder
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    @Test
    void generarTokenCorrectamente() {

        String token = jwtService.generarToken(1L, CORREO);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void validarTokenCorrecto() {

        String token = jwtService.generarToken(1L, CORREO);

        assertTrue(jwtService.validarToken(token));
    }

    @Test
    void obtenerUsuarioIdCorrectamente() {

        String token = jwtService.generarToken(15L, CORREO);

        assertEquals(15L, jwtService.obtenerUsuarioId(token));
    }

    @Test
    void obtenerCorreoCorrectamente() {

        String token = jwtService.generarToken(1L, CORREO);

        assertEquals(CORREO, jwtService.obtenerCorreo(token));
    }

    @Test
    void rechazarTokenInvalido() {

        assertFalse(jwtService.validarToken("token-invalido"));
    }

    @Test
    void rechazarTokenManipulado() {

        String token = jwtService.generarToken(1L, CORREO);

        String[] partes = token.split("\\.");

        String firma = partes[2];

        String firmaModificada =
                (firma.startsWith("A") ? "B" : "A")
                        + firma.substring(1);

        String manipulado = partes[0] + "."
                + partes[1] + "." + firmaModificada;

        assertFalse(jwtService.validarToken(manipulado));
    }

    @Test
    void rechazarTokenVencido() {

        String token = generarToken(
                "1", CORREO, EMISOR, -60000L
        );

        assertFalse(jwtService.validarToken(token));
    }

    @Test
    void rechazarTokenSinExpiracion() {

        String token = generarToken(
                "1", CORREO, EMISOR, null
        );

        assertFalse(jwtService.validarToken(token));
    }

    @Test
    void rechazarTokenSinIdentificador() {

        String token = generarToken(
                null, CORREO, EMISOR, DURACION
        );

        assertFalse(jwtService.validarToken(token));
    }

    @Test
    void rechazarIdentificadorCero() {

        String token = generarToken(
                "0", CORREO, EMISOR, DURACION
        );

        assertFalse(jwtService.validarToken(token));
    }

    @Test
    void rechazarIdentificadorNegativo() {

        String token = generarToken(
                "-1", CORREO, EMISOR, DURACION
        );

        assertFalse(jwtService.validarToken(token));
    }

    @Test
    void rechazarIdentificadorNoNumerico() {

        String token = generarToken(
                "usuario", CORREO, EMISOR, DURACION
        );

        assertFalse(jwtService.validarToken(token));
    }

    @Test
    void rechazarTokenSinCorreo() {

        String token = generarToken(
                "1", null, EMISOR, DURACION
        );

        assertFalse(jwtService.validarToken(token));
    }

    @Test
    void rechazarTokenConCorreoVacio() {

        String token = generarToken(
                "1", "", EMISOR, DURACION
        );

        assertFalse(jwtService.validarToken(token));
    }

    @Test
    void rechazarEmisorIncorrecto() {

        String token = generarToken(
                "1", CORREO, "otro-servicio", DURACION
        );

        assertFalse(jwtService.validarToken(token));
    }

    @Test
    void rechazarAlgoritmoRS512() {

        String token = Jwts.builder()
                .subject("1")
                .claim("correo", CORREO)
                .issuer(EMISOR)
                .expiration(new Date(
                        System.currentTimeMillis() + DURACION
                ))
                .signWith(privateKey, Jwts.SIG.RS512)
                .compact();

        assertFalse(jwtService.validarToken(token));
    }

    @Test
    void rechazarFirmaConOtraClave() throws Exception {

        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA");

        generator.initialize(2048);

        PrivateKey otraClave =
                generator.generateKeyPair().getPrivate();

        String token = Jwts.builder()
                .subject("1")
                .claim("correo", CORREO)
                .issuer(EMISOR)
                .expiration(new Date(
                        System.currentTimeMillis() + DURACION
                ))
                .signWith(otraClave, Jwts.SIG.RS256)
                .compact();

        assertFalse(jwtService.validarToken(token));
    }

    @Test
    void impedirGeneracionConIdNulo() {

        assertThrows(
                IllegalArgumentException.class,
                () -> jwtService.generarToken(null, CORREO)
        );
    }

    @Test
    void impedirGeneracionConIdCero() {

        assertThrows(
                IllegalArgumentException.class,
                () -> jwtService.generarToken(0L, CORREO)
        );
    }

    @Test
    void impedirGeneracionConIdNegativo() {

        assertThrows(
                IllegalArgumentException.class,
                () -> jwtService.generarToken(-1L, CORREO)
        );
    }

    @Test
    void impedirGeneracionConCorreoNulo() {

        assertThrows(
                IllegalArgumentException.class,
                () -> jwtService.generarToken(1L, null)
        );
    }

    @Test
    void impedirGeneracionConCorreoVacio() {

        assertThrows(
                IllegalArgumentException.class,
                () -> jwtService.generarToken(1L, "")
        );
    }

    @Test
    void rechazarConfiguracionConDuracionNegativa() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService(
                        System.getenv("JWT_KEYSTORE_PATH"),
                        System.getenv("JWT_KEYSTORE_PASSWORD"),
                        "martini-jwt",
                        -60000,
                        EMISOR
                )
        );
    }

    @Test
    void rechazarConfiguracionConDuracionCero() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService(
                        System.getenv("JWT_KEYSTORE_PATH"),
                        System.getenv("JWT_KEYSTORE_PASSWORD"),
                        "martini-jwt",
                        0,
                        EMISOR
                )
        );
    }

    @Test
    void rechazarConfiguracionConEmisorVacio() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService(
                        System.getenv("JWT_KEYSTORE_PATH"),
                        System.getenv("JWT_KEYSTORE_PASSWORD"),
                        "martini-jwt",
                        DURACION,
                        ""
                )
        );
    }
}