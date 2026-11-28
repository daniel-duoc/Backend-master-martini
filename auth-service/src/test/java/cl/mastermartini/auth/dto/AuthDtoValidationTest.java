package cl.mastermartini.auth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    private static final String NOMBRE = "Usuario Prueba";
    private static final String CORREO = "prueba@example.com";
    private static final String PASSWORD = "Password123";

    @BeforeAll
    static void configurar() {

        factory = Validation.buildDefaultValidatorFactory();

        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrar() {

        if (factory != null) {
            factory.close();
        }
    }

    // Metodo auxiliar para comprobar una propiedad invalida

    private void comprobarError(
            Object request,
            String propiedad
    ) {

        assertTrue(
                validator.validate(request)
                        .stream()
                        .anyMatch(error ->
                                error.getPropertyPath()
                                        .toString()
                                        .equals(propiedad))
        );
    }

    // PU-DTO-01: Registro valido
    @Test
    void aceptarRegistroValido() {

        RegistroRequest request = new RegistroRequest(
                NOMBRE,
                CORREO,
                PASSWORD
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-02: Normalizar nombre y correo
    @Test
    void normalizarDatosRegistro() {

        RegistroRequest request = new RegistroRequest(
                " Usuario Prueba ",
                " PRUEBA@EXAMPLE.COM ",
                PASSWORD
        );

        assertEquals(NOMBRE, request.nombre());
        assertEquals(CORREO, request.correo());
        assertEquals(PASSWORD, request.password());

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-03: Rechazar nombre vacio
    @Test
    void rechazarNombreVacio() {

        RegistroRequest request = new RegistroRequest(
                "   ",
                CORREO,
                PASSWORD
        );

        comprobarError(request, "nombre");
    }

    // PU-DTO-04: Rechazar nombre nulo
    @Test
    void rechazarNombreNulo() {

        RegistroRequest request = new RegistroRequest(
                null,
                CORREO,
                PASSWORD
        );

        comprobarError(request, "nombre");
    }

    // PU-DTO-05: Rechazar nombre demasiado largo
    @Test
    void rechazarNombreDemasiadoLargo() {

        RegistroRequest request = new RegistroRequest(
                "a".repeat(256),
                CORREO,
                PASSWORD
        );

        comprobarError(request, "nombre");
    }

    // PU-DTO-06: Aceptar nombre de 255 caracteres
    @Test
    void aceptarNombreDe255Caracteres() {

        RegistroRequest request = new RegistroRequest(
                "a".repeat(255),
                CORREO,
                PASSWORD
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-07: Rechazar correo invalido
    @Test
    void rechazarCorreoInvalido() {

        RegistroRequest request = new RegistroRequest(
                NOMBRE,
                "correo-invalido",
                PASSWORD
        );

        comprobarError(request, "correo");
    }

    // PU-DTO-08: Rechazar correo vacio
    @Test
    void rechazarCorreoVacio() {

        RegistroRequest request = new RegistroRequest(
                NOMBRE,
                "   ",
                PASSWORD
        );

        comprobarError(request, "correo");
    }

    // PU-DTO-09: Rechazar correo demasiado largo
    @Test
    void rechazarCorreoDemasiadoLargo() {

        RegistroRequest request = new RegistroRequest(
                NOMBRE,
                "a".repeat(256) + "@example.com",
                PASSWORD
        );

        comprobarError(request, "correo");
    }

    // PU-DTO-10: Rechazar contrasena demasiado corta
    @Test
    void rechazarPasswordCorta() {

        RegistroRequest request = new RegistroRequest(
                NOMBRE,
                CORREO,
                "1234567"
        );

        comprobarError(request, "password");
    }

    // PU-DTO-11: Aceptar contrasena de 8 caracteres
    @Test
    void aceptarPasswordDe8Caracteres() {

        RegistroRequest request = new RegistroRequest(
                NOMBRE,
                CORREO,
                "12345678"
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-12: Aceptar contrasena de 72 bytes
    @Test
    void aceptarPasswordDe72Bytes() {

        RegistroRequest request = new RegistroRequest(
                NOMBRE,
                CORREO,
                "a".repeat(72)
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-13: Rechazar contrasena de 73 bytes
    @Test
    void rechazarPasswordDe73Bytes() {

        RegistroRequest request = new RegistroRequest(
                NOMBRE,
                CORREO,
                "a".repeat(73)
        );

        comprobarError(request, "passwordLongitudValida");
    }

    // PU-DTO-14: Aceptar caracteres multibyte
    @Test
    void aceptarPasswordMultibyteDe72Bytes() {

        RegistroRequest request = new RegistroRequest(
                NOMBRE,
                CORREO,
                "\u00E1".repeat(36)
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-15: Rechazar caracteres multibyte excesivos
    @Test
    void rechazarPasswordMultibyteDe74Bytes() {

        RegistroRequest request = new RegistroRequest(
                NOMBRE,
                CORREO,
                "\u00E1".repeat(37)
        );

        comprobarError(request, "passwordLongitudValida");
    }

    // PU-DTO-16: Rechazar contrasena nula
    @Test
    void rechazarPasswordNula() {

        RegistroRequest request = new RegistroRequest(
                NOMBRE,
                CORREO,
                null
        );

        comprobarError(request, "password");
    }

    // PU-DTO-17: Conservar espacios de la contrasena
    @Test
    void conservarPasswordOriginal() {

        String passwordOriginal = " Password123 ";

        RegistroRequest request = new RegistroRequest(
                NOMBRE,
                CORREO,
                passwordOriginal
        );

        assertEquals(passwordOriginal, request.password());

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-18: Login valido
    @Test
    void aceptarLoginValido() {

        LoginRequest request = new LoginRequest(
                CORREO,
                PASSWORD
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-19: Normalizar correo durante login
    @Test
    void normalizarCorreoLogin() {

        LoginRequest request = new LoginRequest(
                " PRUEBA@EXAMPLE.COM ",
                PASSWORD
        );

        assertEquals(CORREO, request.correo());

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-20: Rechazar correo invalido durante login
    @Test
    void rechazarCorreoInvalidoLogin() {

        LoginRequest request = new LoginRequest(
                "correo-invalido",
                PASSWORD
        );

        comprobarError(request, "correo");
    }

    // PU-DTO-21: Rechazar contrasena vacia durante login
    @Test
    void rechazarPasswordVaciaLogin() {

        LoginRequest request = new LoginRequest(
                CORREO,
                "   "
        );

        comprobarError(request, "password");
    }

    // PU-DTO-22: Permitir validar contrasena corta en login
    @Test
    void aceptarPasswordCortaLogin() {

        LoginRequest request = new LoginRequest(
                CORREO,
                "123"
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-23: Aceptar limite BCrypt durante login
    @Test
    void aceptarPasswordDe72BytesLogin() {

        LoginRequest request = new LoginRequest(
                CORREO,
                "a".repeat(72)
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-24: Rechazar exceso de bytes durante login
    @Test
    void rechazarPasswordDe73BytesLogin() {

        LoginRequest request = new LoginRequest(
                CORREO,
                "a".repeat(73)
        );

        comprobarError(request, "passwordLongitudValida");
    }

    // PU-DTO-25: Rechazar exceso multibyte durante login
    @Test
    void rechazarPasswordMultibyteLogin() {

        LoginRequest request = new LoginRequest(
                CORREO,
                "\u00E1".repeat(37)
        );

        comprobarError(request, "passwordLongitudValida");
    }

    // PU-DTO-26: Rechazar contrasena nula durante login
    @Test
    void rechazarPasswordNulaLogin() {

        LoginRequest request = new LoginRequest(
                CORREO,
                null
        );

        comprobarError(request, "password");
    }

    // PU-DTO-27: Conservar contrasena original durante login
    @Test
    void conservarPasswordOriginalLogin() {

        String passwordOriginal = " Password123 ";

        LoginRequest request = new LoginRequest(
                CORREO,
                passwordOriginal
        );

        assertEquals(passwordOriginal, request.password());

        assertTrue(validator.validate(request).isEmpty());
    }
}
