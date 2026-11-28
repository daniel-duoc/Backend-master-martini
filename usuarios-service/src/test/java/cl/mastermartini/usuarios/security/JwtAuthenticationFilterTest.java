package cl.mastermartini.usuarios.security;

import io.jsonwebtoken.MalformedJwtException;

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

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void configurar() {

        SecurityContextHolder.clearContext();

        jwtService = mock(JwtService.class);

        filter = new JwtAuthenticationFilter(jwtService);

        request = new MockHttpServletRequest();

        request.setMethod("GET");
        request.setServletPath("/api/usuarios/me");

        response = new MockHttpServletResponse();

        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    void limpiar() {

        SecurityContextHolder.clearContext();
    }

    @Test
    void autenticarUsuarioConTokenValido() throws Exception {

        request.addHeader(
                "Authorization",
                "Bearer token-valido"
        );

        when(jwtService.obtenerUsuarioId("token-valido"))
                .thenReturn(15L);

        filter.doFilter(request, response, filterChain);

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals("15", authentication.getName());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void asignarRolUserCorrectamente() throws Exception {

        request.addHeader(
                "Authorization",
                "Bearer token-valido"
        );

        when(jwtService.obtenerUsuarioId("token-valido"))
                .thenReturn(1L);

        filter.doFilter(request, response, filterChain);

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        assertNotNull(authentication);

        assertEquals(1, authentication.getAuthorities().size());

        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_USER"))
        );
    }

    @Test
    void validarTokenUnaSolaVez() throws Exception {

        request.addHeader(
                "Authorization",
                "Bearer token-valido"
        );

        when(jwtService.obtenerUsuarioId("token-valido"))
                .thenReturn(1L);

        filter.doFilter(request, response, filterChain);

        verify(jwtService, times(1))
                .obtenerUsuarioId("token-valido");

        verifyNoMoreInteractions(jwtService);

        verify(filterChain, times(1))
                .doFilter(request, response);
    }

    @Test
    void rechazarTokenInvalido() throws Exception {

        request.addHeader(
                "Authorization",
                "Bearer token-invalido"
        );

        when(jwtService.obtenerUsuarioId("token-invalido"))
                .thenThrow(new MalformedJwtException(
                        "Token invalido"
                ));

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verifyNoInteractions(filterChain);
    }

    @Test
    void rechazarIdentificadorInvalido() throws Exception {

        request.addHeader(
                "Authorization",
                "Bearer token-invalido"
        );

        when(jwtService.obtenerUsuarioId("token-invalido"))
                .thenThrow(new IllegalArgumentException(
                        "Identificador invalido"
                ));

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verifyNoInteractions(filterChain);
    }

    @Test
    void continuarSinToken() throws Exception {

        filter.doFilter(request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verifyNoInteractions(jwtService);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void ignorarEndpointInterno() throws Exception {

        request.setMethod("POST");
        request.setServletPath("/api/usuarios/interno");

        request.addHeader(
                "Authorization",
                "Bearer token-invalido"
        );

        filter.doFilter(request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verifyNoInteractions(jwtService);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void aceptarBearerEnMinusculas() throws Exception {

        request.addHeader(
                "Authorization",
                "bearer token-valido"
        );

        when(jwtService.obtenerUsuarioId("token-valido"))
                .thenReturn(1L);

        filter.doFilter(request, response, filterChain);

        assertNotNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verify(jwtService)
                .obtenerUsuarioId("token-valido");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void ignorarClaveInternaComoAutenticacionJwt()
            throws Exception {

        request.addHeader(
                "X-Internal-Key",
                "clave-exclusiva-de-prueba"
        );

        filter.doFilter(request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );

        verifyNoInteractions(jwtService);

        verify(filterChain).doFilter(request, response);
    }
}