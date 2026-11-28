package cl.mastermartini.usuarios.security;

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

        String publicKeyPath =
                System.getenv("JWT_PUBLIC_KEY_PATH");

        String keystorePath =
                System.getenv("JWT_KEYSTORE_PATH");

        String keystorePassword =
                System.getenv("JWT_KEYSTORE_PASSWORD");

        assertNotNull(publicKeyPath);
        assertNotNull(keystorePath);
        assertNotNull(keystorePassword);

        jwtService = new JwtService(
                publicKeyPath,
                EMISOR
        );

        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (InputStream input =
                     Files.newInputStream(Path.of(keystorePath))) {

            keyStore.load(
                    input,
                    keystorePassword.toCharArray()
            );
        }

        privateKey = (PrivateKey) keyStore.getKey(
                "martini-jwt",
                keystorePassword.toCharArray()
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
    void validarTokenCorrecto() {

        String token = generarToken(
                "1", CORREO, EMISOR, DURACION
        );

        assertTrue(jwtService.validarToken(token));
    }

    @Test
    void obtenerUsuarioIdCorrectamente() {

        String token = generarToken(
                "15", CORREO, EMISOR, DURACION
        );

        assertEquals(15L, jwtService.obtenerUsuarioId(token));
    }

    @Test
    void obtenerCorreoCorrectamente() {

        String token = generarToken(
                "1", CORREO, EMISOR, DURACION
        );

        assertEquals(CORREO, jwtService.obtenerCorreo(token));
    }

    @Test
    void rechazarTokenExpirado() {

        String token = generarToken(
                "1", CORREO, EMISOR, -60000L
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
    void rechazarTokenInvalido() {

        assertFalse(
                jwtService.validarToken("token-invalido")
        );
    }

    @Test
    void rechazarFirmaIncorrecta() throws Exception {

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
    void rechazarTokenManipulado() {

        String token = generarToken(
                "1", CORREO, EMISOR, DURACION
        );

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
    void rechazarConfiguracionConRutaVacia() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService("", EMISOR)
        );
    }

    @Test
    void rechazarConfiguracionConEmisorVacio() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService(
                        System.getenv("JWT_PUBLIC_KEY_PATH"),
                        ""
                )
        );
    }
}