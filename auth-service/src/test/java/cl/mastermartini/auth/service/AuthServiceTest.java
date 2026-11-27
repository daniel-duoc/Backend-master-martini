
package cl.mastermartini.auth.service;

import cl.mastermartini.auth.dto.AuthResponse;
import cl.mastermartini.auth.dto.LoginRequest;
import cl.mastermartini.auth.dto.RegistroRequest;
import cl.mastermartini.auth.model.Credencial;
import cl.mastermartini.auth.repository.CredencialRepository;
import cl.mastermartini.auth.security.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
    void configurar() {
        credenciales = mock(CredencialRepository.class);
        encoder = mock(PasswordEncoder.class);
        jwt = mock(JwtService.class);

        authService = new AuthService(
                credenciales, encoder, jwt);
    }

    // PU-AUTH-01: Registro correcto
    @Test
    void registrarCorrectamente() {

        RegistroRequest request = new RegistroRequest(
                "Usuario Prueba",
                "prueba@example.com",
                "Password123"
        );

        Credencial credencial = new Credencial(
                "prueba@example.com",
                "password-protegida"
        );

        credencial.setId(1L);

        when(credenciales.existsByCorreo("prueba@example.com"))
                .thenReturn(false);

        when(encoder.encode("Password123"))
                .thenReturn("password-protegida");

        when(credenciales.save(any(Credencial.class)))
                .thenReturn(credencial);

        when(jwt.generarToken(1L, "prueba@example.com"))
                .thenReturn("token-prueba");

        AuthResponse respuesta = authService.registrar(request);

        assertEquals("token-prueba", respuesta.token());
        assertEquals("Usuario Prueba", respuesta.nombre());
        assertEquals("prueba@example.com", respuesta.correo());

        verify(encoder).encode("Password123");

        verify(credenciales).save(argThat(c ->
                "prueba@example.com".equals(c.getCorreo())
                        && "password-protegida".equals(c.getPasswordHash())
        ));

        verify(jwt).generarToken(1L, "prueba@example.com");
    }

    // PU-AUTH-02: Registro con correo duplicado
    @Test
    void rechazarCorreoDuplicado() {

        RegistroRequest request = new RegistroRequest(
                "Usuario Prueba",
                "prueba@example.com",
                "Password123"
        );

        when(credenciales.existsByCorreo("prueba@example.com"))
                .thenReturn(true);

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.registrar(request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                excepcion.getStatusCode()
        );

        verify(credenciales, never())
                .save(any(Credencial.class));

        verifyNoInteractions(encoder, jwt);
    }

    // PU-AUTH-03: Normalizacion del correo
    @Test
    void normalizarCorreo() {

        RegistroRequest request = new RegistroRequest(
                " Usuario Prueba ",
                " PRUEBA@EXAMPLE.COM ",
                "Password123"
        );

        Credencial credencial = new Credencial(
                "prueba@example.com",
                "password-protegida"
        );

        credencial.setId(1L);

        when(credenciales.existsByCorreo("prueba@example.com"))
                .thenReturn(false);

        when(encoder.encode("Password123"))
                .thenReturn("password-protegida");

        when(credenciales.save(any(Credencial.class)))
                .thenReturn(credencial);

        when(jwt.generarToken(1L, "prueba@example.com"))
                .thenReturn("token-prueba");

        AuthResponse respuesta = authService.registrar(request);

        assertEquals("prueba@example.com", respuesta.correo());
        assertEquals("Usuario Prueba", respuesta.nombre());

        verify(credenciales)
                .existsByCorreo("prueba@example.com");

        verify(credenciales).save(argThat(c ->
                "prueba@example.com".equals(c.getCorreo())
        ));
    }

    // PU-AUTH-04: Inicio de sesion correcto
    @Test
    void iniciarSesionCorrectamente() {

        LoginRequest request = new LoginRequest(
                "prueba@example.com",
                "Password123"
        );

        Credencial credencial = new Credencial(
                "prueba@example.com",
                "password-protegida"
        );

        credencial.setId(1L);

        when(credenciales.findByCorreo("prueba@example.com"))
                .thenReturn(Optional.of(credencial));

        when(encoder.matches(
                "Password123", "password-protegida"))
                .thenReturn(true);

        when(jwt.generarToken(1L, "prueba@example.com"))
                .thenReturn("token-prueba");

        AuthResponse respuesta = authService.login(request);

        assertEquals("token-prueba", respuesta.token());
        assertEquals("prueba@example.com", respuesta.correo());
        assertEquals("", respuesta.nombre());

        verify(jwt).generarToken(1L, "prueba@example.com");
    }

    // PU-AUTH-05: Contraseña incorrecta
    @Test
    void rechazarPasswordIncorrecta() {

        LoginRequest request = new LoginRequest(
                "prueba@example.com",
                "PasswordIncorrecta"
        );

        Credencial credencial = new Credencial(
                "prueba@example.com",
                "password-protegida"
        );

        credencial.setId(1L);

        when(credenciales.findByCorreo("prueba@example.com"))
                .thenReturn(Optional.of(credencial));

        when(encoder.matches(
                "PasswordIncorrecta", "password-protegida"))
                .thenReturn(false);

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(request)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                excepcion.getStatusCode()
        );

        verifyNoInteractions(jwt);
    }

    // PU-AUTH-06: Correo no registrado
    @Test
    void rechazarCorreoNoRegistrado() {

        LoginRequest request = new LoginRequest(
                "inexistente@example.com",
                "Password123"
        );

        when(credenciales.findByCorreo("inexistente@example.com"))
                .thenReturn(Optional.empty());

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(request)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                excepcion.getStatusCode()
        );

        verifyNoInteractions(encoder, jwt);
    }
}

