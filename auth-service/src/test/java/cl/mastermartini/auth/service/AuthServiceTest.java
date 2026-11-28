package cl.mastermartini.auth.service;

import cl.mastermartini.auth.client.UsuariosClient;
import cl.mastermartini.auth.dto.AuthResponse;
import cl.mastermartini.auth.dto.LoginRequest;
import cl.mastermartini.auth.dto.RegistroRequest;
import cl.mastermartini.auth.dto.PerfilResponse;
import cl.mastermartini.auth.model.Credencial;
import cl.mastermartini.auth.model.EstadoRegistro;
import cl.mastermartini.auth.repository.CredencialRepository;
import cl.mastermartini.auth.security.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private CredencialRepository credenciales;
    private PasswordEncoder encoder;
    private JwtService jwt;
    private AuthService authService;
    private UsuariosClient usuariosClient;
    private RegistroTransaccionalService registroTransaccional;

    private static final String CORREO = "prueba@example.com";
    private static final String NOMBRE = "Usuario Prueba";
    private static final String PASSWORD = "Password123";
    private static final String HASH = "password-protegida";
    private static final String TOKEN = "token-prueba";

    @BeforeEach
    void configurar() {

        credenciales = mock(CredencialRepository.class);
        encoder = mock(PasswordEncoder.class);
        jwt = mock(JwtService.class);
        usuariosClient = mock(UsuariosClient.class);

        registroTransaccional =
                mock(RegistroTransaccionalService.class);

        authService = new AuthService(
                credenciales,
                encoder,
                jwt,
                usuariosClient,
                registroTransaccional
        );
    }

    // Metodos auxiliares para preparar las pruebas

    private RegistroRequest solicitudRegistro() {

        return new RegistroRequest(
                NOMBRE,
                CORREO,
                PASSWORD
        );
    }

    private LoginRequest solicitudLogin() {

        return new LoginRequest(
                CORREO,
                PASSWORD
        );
    }

    private Credencial credencialPendiente() {

        Credencial credencial = new Credencial(
                CORREO,
                HASH,
                NOMBRE
        );

        credencial.setId(1L);
        credencial.setEstadoRegistro(EstadoRegistro.PENDIENTE);

        return credencial;
    }

    private Credencial credencialActiva() {

        Credencial credencial = credencialPendiente();

        credencial.setEstadoRegistro(EstadoRegistro.ACTIVO);
        credencial.setNombreRegistro(null);

        return credencial;
    }

    private void prepararRegistroNuevo() {

        when(credenciales.findByCorreo(CORREO))
                .thenReturn(Optional.empty());

        when(encoder.encode(PASSWORD))
                .thenReturn(HASH);

        when(registroTransaccional.crearPendiente(
                CORREO, HASH, NOMBRE))
                .thenReturn(credencialPendiente());

        when(jwt.generarToken(1L, CORREO))
                .thenReturn(TOKEN);
    }

    private void prepararLoginValido() {

        when(credenciales.findByCorreo(CORREO))
                .thenReturn(Optional.of(credencialActiva()));

        when(encoder.matches(PASSWORD, HASH))
                .thenReturn(true);

        when(jwt.generarToken(1L, CORREO))
                .thenReturn(TOKEN);
    }

    private void prepararRegistroPendiente() {

        when(credenciales.findByCorreo(CORREO))
                .thenReturn(Optional.of(credencialPendiente()));

        when(encoder.matches(PASSWORD, HASH))
                .thenReturn(true);

        when(jwt.generarToken(1L, CORREO))
                .thenReturn(TOKEN);
    }

    // PU-AUTH-01: Registro correcto
    @Test
    void registrarCorrectamente() {

        prepararRegistroNuevo();

        AuthResponse respuesta =
                authService.registrar(solicitudRegistro());

        assertEquals(TOKEN, respuesta.token());
        assertEquals(NOMBRE, respuesta.nombre());
        assertEquals(CORREO, respuesta.correo());

        verify(encoder).encode(PASSWORD);

        verify(registroTransaccional)
                .crearPendiente(CORREO, HASH, NOMBRE);

        verify(registroTransaccional)
                .activarRegistro(1L);

        verify(jwt).generarToken(1L, CORREO);
    }

    // PU-AUTH-02: Registro con correo de cuenta activa
    @Test
    void rechazarCorreoDuplicado() {

        when(credenciales.findByCorreo(CORREO))
                .thenReturn(Optional.of(credencialActiva()));

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.registrar(solicitudRegistro())
        );

        assertEquals(
                HttpStatus.CONFLICT,
                excepcion.getStatusCode()
        );

        verifyNoInteractions(
                encoder,
                jwt,
                usuariosClient,
                registroTransaccional
        );
    }

    // PU-AUTH-03: Normalizacion del correo y nombre
    @Test
    void normalizarCorreo() {

        prepararRegistroNuevo();

        RegistroRequest request = new RegistroRequest(
                " Usuario Prueba ",
                " PRUEBA@EXAMPLE.COM ",
                PASSWORD
        );

        AuthResponse respuesta = authService.registrar(request);

        assertEquals(CORREO, respuesta.correo());
        assertEquals(NOMBRE, respuesta.nombre());

        verify(credenciales).findByCorreo(CORREO);

        verify(registroTransaccional)
                .crearPendiente(CORREO, HASH, NOMBRE);
    }

    // PU-AUTH-04: Inicio de sesion correcto
    @Test
    void iniciarSesionCorrectamente() {

        prepararLoginValido();

        when(usuariosClient.obtenerPerfil(TOKEN))
                .thenReturn(new PerfilResponse(
                        1L,
                        NOMBRE,
                        CORREO
                ));

        AuthResponse respuesta =
                authService.login(solicitudLogin());

        assertEquals(TOKEN, respuesta.token());
        assertEquals(CORREO, respuesta.correo());
        assertEquals(NOMBRE, respuesta.nombre());

        verify(jwt).generarToken(1L, CORREO);

        verify(usuariosClient).obtenerPerfil(TOKEN);
    }

    // PU-AUTH-05: Contrasena incorrecta
    @Test
    void rechazarPasswordIncorrecta() {

        when(credenciales.findByCorreo(CORREO))
                .thenReturn(Optional.of(credencialActiva()));

        when(encoder.matches("PasswordIncorrecta", HASH))
                .thenReturn(false);

        LoginRequest request = new LoginRequest(
                CORREO,
                "PasswordIncorrecta"
        );

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(request)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                excepcion.getStatusCode()
        );

        verifyNoInteractions(jwt, usuariosClient);
    }

    // PU-AUTH-06: Correo no registrado
    @Test
    void rechazarCorreoNoRegistrado() {

        when(credenciales.findByCorreo(CORREO))
                .thenReturn(Optional.empty());

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(solicitudLogin())
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                excepcion.getStatusCode()
        );

        verifyNoInteractions(
                encoder,
                jwt,
                usuariosClient,
                registroTransaccional
        );
    }

    // PU-AUTH-07: Orden correcto del registro
    @Test
    void registroCreaPerfilUsuario() {

        prepararRegistroNuevo();

        authService.registrar(solicitudRegistro());

        InOrder orden = inOrder(
                registroTransaccional,
                usuariosClient,
                jwt
        );

        orden.verify(registroTransaccional)
                .crearPendiente(CORREO, HASH, NOMBRE);

        orden.verify(usuariosClient)
                .crearPerfil(1L, NOMBRE, CORREO);

        orden.verify(registroTransaccional)
                .activarRegistro(1L);

        orden.verify(jwt)
                .generarToken(1L, CORREO);
    }

    // PU-AUTH-08: Fallo en la creacion del perfil
    @Test
    void registroFallaSiUsuariosServiceNoDisponible() {

        prepararRegistroNuevo();

        doThrow(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo crear el perfil"
        )).when(usuariosClient)
                .crearPerfil(1L, NOMBRE, CORREO);

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.registrar(solicitudRegistro())
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                excepcion.getStatusCode()
        );

        verify(registroTransaccional)
                .crearPendiente(CORREO, HASH, NOMBRE);

        verify(registroTransaccional, never())
                .activarRegistro(anyLong());

        verifyNoInteractions(jwt);
    }

    // PU-AUTH-09: Login con usuarios-service no disponible
    @Test
    void loginFallaSiUsuariosServiceNoDisponible() {

        prepararLoginValido();

        when(usuariosClient.obtenerPerfil(TOKEN))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo consultar el perfil"
                ));

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(solicitudLogin())
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                excepcion.getStatusCode()
        );

        verify(usuariosClient).obtenerPerfil(TOKEN);
    }

    // PU-AUTH-10: Perfil con ID incorrecto
    @Test
    void rechazarPerfilConIdIncorrecto() {

        prepararLoginValido();

        when(usuariosClient.obtenerPerfil(TOKEN))
                .thenReturn(new PerfilResponse(
                        99L,
                        "Otro Usuario",
                        CORREO
                ));

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(solicitudLogin())
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                excepcion.getStatusCode()
        );
    }

    // PU-AUTH-11: Perfil con correo incorrecto
    @Test
    void rechazarPerfilConCorreoIncorrecto() {

        prepararLoginValido();

        when(usuariosClient.obtenerPerfil(TOKEN))
                .thenReturn(new PerfilResponse(
                        1L,
                        "Otro Usuario",
                        "otro@example.com"
                ));

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(solicitudLogin())
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                excepcion.getStatusCode()
        );
    }

    // PU-AUTH-12: Recuperar un registro pendiente
    @Test
    void recuperarRegistroPendiente() {

        prepararRegistroPendiente();

        AuthResponse respuesta =
                authService.registrar(solicitudRegistro());

        assertEquals(TOKEN, respuesta.token());
        assertEquals(NOMBRE, respuesta.nombre());
        assertEquals(CORREO, respuesta.correo());

        verify(registroTransaccional, never())
                .crearPendiente(
                        anyString(),
                        anyString(),
                        anyString()
                );

        verify(encoder, never()).encode(anyString());

        verify(usuariosClient)
                .crearPerfil(1L, NOMBRE, CORREO);

        verify(registroTransaccional)
                .activarRegistro(1L);
    }

    // PU-AUTH-13: Recuperacion con contrasena incorrecta
    @Test
    void rechazarRecuperacionConPasswordIncorrecta() {

        when(credenciales.findByCorreo(CORREO))
                .thenReturn(Optional.of(credencialPendiente()));

        when(encoder.matches("PasswordIncorrecta", HASH))
                .thenReturn(false);

        RegistroRequest request = new RegistroRequest(
                NOMBRE,
                CORREO,
                "PasswordIncorrecta"
        );

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.registrar(request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                excepcion.getStatusCode()
        );

        verifyNoInteractions(
                jwt,
                usuariosClient,
                registroTransaccional
        );
    }

    // PU-AUTH-14: Impedir login de una cuenta pendiente
    @Test
    void rechazarLoginDeCuentaPendiente() {

        when(credenciales.findByCorreo(CORREO))
                .thenReturn(Optional.of(credencialPendiente()));

        when(encoder.matches(PASSWORD, HASH))
                .thenReturn(true);

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(solicitudLogin())
        );

        assertEquals(
                HttpStatus.CONFLICT,
                excepcion.getStatusCode()
        );

        verifyNoInteractions(
                jwt,
                usuariosClient,
                registroTransaccional
        );
    }

    // PU-AUTH-15: Recuperar una insercion concurrente
    @Test
    void recuperarRegistroConcurrente() {

        when(credenciales.findByCorreo(CORREO))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(credencialPendiente()));

        when(encoder.encode(PASSWORD))
                .thenReturn(HASH);

        when(registroTransaccional.crearPendiente(
                CORREO, HASH, NOMBRE))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Conflicto de registro"
                ));

        when(encoder.matches(PASSWORD, HASH))
                .thenReturn(true);

        when(jwt.generarToken(1L, CORREO))
                .thenReturn(TOKEN);

        AuthResponse respuesta =
                authService.registrar(solicitudRegistro());

        assertEquals(TOKEN, respuesta.token());

        verify(credenciales, times(2))
                .findByCorreo(CORREO);

        verify(usuariosClient)
                .crearPerfil(1L, NOMBRE, CORREO);

        verify(registroTransaccional)
                .activarRegistro(1L);
    }

    // PU-AUTH-16: Fallo durante la activacion
    @Test
    void noGenerarTokenSiFallaActivacion() {

        prepararRegistroNuevo();

        when(registroTransaccional.activarRegistro(1L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo activar la cuenta"
                ));

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.registrar(solicitudRegistro())
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                excepcion.getStatusCode()
        );

        verify(usuariosClient)
                .crearPerfil(1L, NOMBRE, CORREO);

        verifyNoInteractions(jwt);
    }

    // PU-AUTH-17: Registro pendiente sin nombre
    @Test
    void rechazarRegistroPendienteSinNombre() {

        Credencial credencial = credencialPendiente();

        credencial.setNombreRegistro(null);

        when(credenciales.findByCorreo(CORREO))
                .thenReturn(Optional.of(credencial));

        when(encoder.matches(PASSWORD, HASH))
                .thenReturn(true);

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.registrar(solicitudRegistro())
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                excepcion.getStatusCode()
        );

        verifyNoInteractions(
                jwt,
                usuariosClient,
                registroTransaccional
        );
    }

    // PU-AUTH-18: Conservar el nombre original
    @Test
    void conservarNombreOriginalEnRecuperacion() {

        prepararRegistroPendiente();

        RegistroRequest request = new RegistroRequest(
                "Nombre Diferente",
                CORREO,
                PASSWORD
        );

        AuthResponse respuesta =
                authService.registrar(request);

        assertEquals(NOMBRE, respuesta.nombre());

        verify(usuariosClient)
                .crearPerfil(1L, NOMBRE, CORREO);

        verify(registroTransaccional)
                .activarRegistro(1L);
    }

    // PU-AUTH-19: Conflicto concurrente con cuenta activa
    @Test
    void rechazarConflictoConcurrenteConCuentaActiva() {

        when(credenciales.findByCorreo(CORREO))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(credencialActiva()));

        when(encoder.encode(PASSWORD))
                .thenReturn(HASH);

        when(registroTransaccional.crearPendiente(
                CORREO, HASH, NOMBRE))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Correo duplicado"
                ));

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.registrar(solicitudRegistro())
        );

        assertEquals(
                HttpStatus.CONFLICT,
                excepcion.getStatusCode()
        );

        verify(registroTransaccional, never())
                .activarRegistro(anyLong());

        verifyNoInteractions(jwt, usuariosClient);
    }

    // PU-AUTH-20: Propagar errores distintos de conflicto
    @Test
    void propagarErrorDeCreacionPendiente() {

        when(credenciales.findByCorreo(CORREO))
                .thenReturn(Optional.empty());

        when(encoder.encode(PASSWORD))
                .thenReturn(HASH);

        when(registroTransaccional.crearPendiente(
                CORREO, HASH, NOMBRE))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Base de datos no disponible"
                ));

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.registrar(solicitudRegistro())
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                excepcion.getStatusCode()
        );

        verify(credenciales, times(1))
                .findByCorreo(CORREO);

        verifyNoInteractions(jwt, usuariosClient);
    }
}