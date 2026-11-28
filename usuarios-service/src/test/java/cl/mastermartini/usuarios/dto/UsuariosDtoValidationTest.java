package cl.mastermartini.usuarios.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsuariosDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    private static final Long ID = 1L;
    private static final String NOMBRE = "Usuario Prueba";
    private static final String CORREO = "prueba@example.com";

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

    // Metodo auxiliar para comprobar errores de validacion

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
                                        .equals(propiedad)),
                "Se esperaba un error en: " + propiedad
        );
    }

    // Metodo auxiliar para crear una actualizacion basica

    private ActualizarPerfilRequest actualizacionBasica(
            String nombre
    ) {

        return new ActualizarPerfilRequest(
                nombre,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    // ==========================================
    // PRUEBAS DE CREAR PERFIL
    // ==========================================

    // PU-DTO-USU-01: Creacion valida
    @Test
    void aceptarCreacionValida() {

        CrearPerfilRequest request = new CrearPerfilRequest(
                ID,
                NOMBRE,
                CORREO
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-USU-02: Rechazar ID nulo
    @Test
    void rechazarIdNulo() {

        CrearPerfilRequest request = new CrearPerfilRequest(
                null,
                NOMBRE,
                CORREO
        );

        comprobarError(request, "id");
    }

    // PU-DTO-USU-03: Rechazar ID igual a cero
    @Test
    void rechazarIdCero() {

        CrearPerfilRequest request = new CrearPerfilRequest(
                0L,
                NOMBRE,
                CORREO
        );

        comprobarError(request, "id");
    }

    // PU-DTO-USU-04: Rechazar ID negativo
    @Test
    void rechazarIdNegativo() {

        CrearPerfilRequest request = new CrearPerfilRequest(
                -1L,
                NOMBRE,
                CORREO
        );

        comprobarError(request, "id");
    }

    // PU-DTO-USU-05: Rechazar nombre nulo
    @Test
    void rechazarNombreNulo() {

        CrearPerfilRequest request = new CrearPerfilRequest(
                ID,
                null,
                CORREO
        );

        comprobarError(request, "nombre");
    }

    // PU-DTO-USU-06: Rechazar nombre vacio
    @Test
    void rechazarNombreVacio() {

        CrearPerfilRequest request = new CrearPerfilRequest(
                ID,
                "",
                CORREO
        );

        comprobarError(request, "nombre");
    }

    // PU-DTO-USU-07: Rechazar nombre con solo espacios
    @Test
    void rechazarNombreConEspacios() {

        CrearPerfilRequest request = new CrearPerfilRequest(
                ID,
                "   ",
                CORREO
        );

        comprobarError(request, "nombre");
    }

    // PU-DTO-USU-08: Aceptar nombre de 255 caracteres
    @Test
    void aceptarNombreDe255Caracteres() {

        CrearPerfilRequest request = new CrearPerfilRequest(
                ID,
                "a".repeat(255),
                CORREO
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-USU-09: Rechazar nombre de 256 caracteres
    @Test
    void rechazarNombreDe256Caracteres() {

        CrearPerfilRequest request = new CrearPerfilRequest(
                ID,
                "a".repeat(256),
                CORREO
        );

        comprobarError(request, "nombre");
    }

    // PU-DTO-USU-10: Rechazar correo nulo
    @Test
    void rechazarCorreoNulo() {

        CrearPerfilRequest request = new CrearPerfilRequest(
                ID,
                NOMBRE,
                null
        );

        comprobarError(request, "correo");
    }

    // PU-DTO-USU-11: Rechazar correo vacio
    @Test
    void rechazarCorreoVacio() {

        CrearPerfilRequest request = new CrearPerfilRequest(
                ID,
                NOMBRE,
                ""
        );

        comprobarError(request, "correo");
    }

    // PU-DTO-USU-12: Rechazar correo con solo espacios
    @Test
    void rechazarCorreoConEspacios() {

        CrearPerfilRequest request = new CrearPerfilRequest(
                ID,
                NOMBRE,
                "   "
        );

        comprobarError(request, "correo");
    }

    // PU-DTO-USU-13: Rechazar formato de correo invalido
    @Test
    void rechazarCorreoInvalido() {

        CrearPerfilRequest request = new CrearPerfilRequest(
                ID,
                NOMBRE,
                "correo-invalido"
        );

        comprobarError(request, "correo");
    }

    // PU-DTO-USU-14: Rechazar correo demasiado largo
    @Test
    void rechazarCorreoDemasiadoLargo() {

        CrearPerfilRequest request = new CrearPerfilRequest(
                ID,
                NOMBRE,
                "a".repeat(256) + "@example.com"
        );

        assertTrue(
                validator.validate(request)
                        .stream()
                        .anyMatch(error ->
                                error.getPropertyPath()
                                        .toString()
                                        .equals("correo")
                                        &&
                                        error.getConstraintDescriptor()
                                                .getAnnotation()
                                                .annotationType()
                                                .equals(
                                                        jakarta.validation
                                                                .constraints.Size.class
                                                ))
        );
    }

    // ==========================================
    // PRUEBAS DE ACTUALIZAR PERFIL
    // ==========================================

    // PU-DTO-USU-15: Actualizacion completa valida
    @Test
    void aceptarActualizacionCompleta() {

        ActualizarPerfilRequest request =
                new ActualizarPerfilRequest(
                        NOMBRE,
                        "912345678",
                        "Pasteleria",
                        "Chocolateria",
                        "https://example.com/foto.png",
                        "Pasteleria Prueba",
                        "Santiago",
                        "Administrador",
                        "Empresa de pasteleria"
                );

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-USU-16: Aceptar campos opcionales nulos
    @Test
    void aceptarCamposOpcionalesNulos() {

        ActualizarPerfilRequest request =
                actualizacionBasica(NOMBRE);

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-USU-17: Aceptar campos opcionales vacios
    @Test
    void aceptarCamposOpcionalesVacios() {

        ActualizarPerfilRequest request =
                new ActualizarPerfilRequest(
                        NOMBRE,
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        ""
                );

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-USU-18: Rechazar nombre nulo
    @Test
    void rechazarNombreNuloActualizacion() {

        comprobarError(
                actualizacionBasica(null),
                "nombre"
        );
    }

    // PU-DTO-USU-19: Rechazar nombre vacio
    @Test
    void rechazarNombreVacioActualizacion() {

        comprobarError(
                actualizacionBasica(""),
                "nombre"
        );
    }

    // PU-DTO-USU-20: Rechazar nombre con solo espacios
    @Test
    void rechazarNombreConEspaciosActualizacion() {

        comprobarError(
                actualizacionBasica("   "),
                "nombre"
        );
    }

    // PU-DTO-USU-21: Aceptar nombre de 255 caracteres
    @Test
    void aceptarNombreMaximoActualizacion() {

        ActualizarPerfilRequest request =
                actualizacionBasica("a".repeat(255));

        assertTrue(validator.validate(request).isEmpty());
    }

    // PU-DTO-USU-22: Rechazar nombre de 256 caracteres
    @Test
    void rechazarNombreDemasiadoLargoActualizacion() {

        comprobarError(
                actualizacionBasica("a".repeat(256)),
                "nombre"
        );
    }

    // PU-DTO-USU-23: Rechazar telefono demasiado largo
    @Test
    void rechazarTelefonoDemasiadoLargo() {

        ActualizarPerfilRequest request =
                new ActualizarPerfilRequest(
                        NOMBRE,
                        "1".repeat(256),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        comprobarError(request, "telefono");
    }

    // PU-DTO-USU-24: Rechazar tipo de negocio demasiado largo
    @Test
    void rechazarTipoNegocioDemasiadoLargo() {

        ActualizarPerfilRequest request =
                new ActualizarPerfilRequest(
                        NOMBRE,
                        null,
                        "a".repeat(256),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        comprobarError(request, "tipoNegocio");
    }

    // PU-DTO-USU-25: Rechazar intereses demasiado largos
    @Test
    void rechazarInteresesDemasiadoLargos() {

        ActualizarPerfilRequest request =
                new ActualizarPerfilRequest(
                        NOMBRE,
                        null,
                        null,
                        "a".repeat(1001),
                        null,
                        null,
                        null,
                        null,
                        null
                );

        comprobarError(request, "intereses");
    }

    // PU-DTO-USU-26: Rechazar foto demasiado larga
    @Test
    void rechazarFotoDemasiadoLarga() {

        ActualizarPerfilRequest request =
                new ActualizarPerfilRequest(
                        NOMBRE,
                        null,
                        null,
                        null,
                        "a".repeat(2049),
                        null,
                        null,
                        null,
                        null
                );

        comprobarError(request, "foto");
    }

    // PU-DTO-USU-27: Rechazar nombre de empresa demasiado largo
    @Test
    void rechazarNombreEmpresaDemasiadoLargo() {

        ActualizarPerfilRequest request =
                new ActualizarPerfilRequest(
                        NOMBRE,
                        null,
                        null,
                        null,
                        null,
                        "a".repeat(256),
                        null,
                        null,
                        null
                );

        comprobarError(request, "nombreEmpresa");
    }

    // PU-DTO-USU-28: Rechazar direccion demasiado larga
    @Test
    void rechazarDireccionEmpresaDemasiadoLarga() {

        ActualizarPerfilRequest request =
                new ActualizarPerfilRequest(
                        NOMBRE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "a".repeat(256),
                        null,
                        null
                );

        comprobarError(request, "direccionEmpresa");
    }

    // PU-DTO-USU-29: Rechazar cargo demasiado largo
    @Test
    void rechazarCargoEmpresaDemasiadoLargo() {

        ActualizarPerfilRequest request =
                new ActualizarPerfilRequest(
                        NOMBRE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "a".repeat(256),
                        null
                );

        comprobarError(request, "cargoEmpresa");
    }

    // PU-DTO-USU-30: Rechazar descripcion demasiado larga
    @Test
    void rechazarDescripcionEmpresaDemasiadoLarga() {

        ActualizarPerfilRequest request =
                new ActualizarPerfilRequest(
                        NOMBRE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "a".repeat(2001)
                );

        comprobarError(request, "descripcionEmpresa");
    }

    // PU-DTO-USU-31: Aceptar todos los limites maximos
    @Test
    void aceptarTodosLosLimitesMaximos() {

        ActualizarPerfilRequest request =
                new ActualizarPerfilRequest(
                        "a".repeat(255),
                        "1".repeat(255),
                        "a".repeat(255),
                        "a".repeat(1000),
                        "a".repeat(2048),
                        "a".repeat(255),
                        "a".repeat(255),
                        "a".repeat(255),
                        "a".repeat(2000)
                );

        assertTrue(validator.validate(request).isEmpty());
    }
}
