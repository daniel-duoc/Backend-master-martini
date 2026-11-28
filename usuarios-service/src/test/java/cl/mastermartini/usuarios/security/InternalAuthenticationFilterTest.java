package cl.mastermartini.usuarios.security;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InternalAuthenticationFilterTest {

    private InternalAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    private static final String CLAVE_PRUEBA =
            "clave-interna-exclusiva-para-pruebas";

    @BeforeEach
    void configurar() {

        SecurityContextHolder.clearContext();

        filter = new InternalAuthenticationFilter(
                CLAVE_PRUEBA
        );

        request = new MockHttpServletRequest();

        request.setMethod("POST");
        request.setServletPath("/api/usuarios/interno");

        response = new MockHttpServletResponse();

        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    void limpiar() {

        SecurityContextHolder.clearContext();
    }

    // PU-USU-15: Clave interna correcta
    @Test
    void aceptarClaveCorrecta() throws Exception {

        request.addHeader(
                "X-Internal-Key",
                CLAVE_PRUEBA
        );

        filter.doFilter(
                request,
                response,
                filterChain
        );

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        assertNotNull(authentication);

        assertTrue(authentication.isAuthenticated());

        assertEquals(
                "auth-service",
                authentication.getName()
        );

        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_INTERNAL"))
        );

        verify(filterChain, times(1))
                .doFilter(request, response);
    }

    // PU-USU-16: Clave interna incorrecta
    @Test
    void rechazarClaveIncorrecta() throws Exception {

        request.addHeader(
                "X-Internal-Key",
                "clave-incorrecta"
        );

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(401, response.getStatus());

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verify(filterChain, never())
                .doFilter(request, response);
    }

    // PU-USU-17: Solicitud sin clave interna
    @Test
    void rechazarSolicitudSinClave() throws Exception {

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(401, response.getStatus());

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verify(filterChain, never())
                .doFilter(request, response);
    }

    // PU-USU-18: No interferir con la consulta de perfil
    @Test
    void ignorarEndpointDePerfil() throws Exception {

        request.setMethod("GET");
        request.setServletPath("/api/usuarios/me");

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verify(filterChain, times(1))
                .doFilter(request, response);
    }

    // PU-USU-19: Rechazar configuracion con clave vacia
    @Test
    void rechazarClaveVacia() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new InternalAuthenticationFilter("")
        );
    }
}