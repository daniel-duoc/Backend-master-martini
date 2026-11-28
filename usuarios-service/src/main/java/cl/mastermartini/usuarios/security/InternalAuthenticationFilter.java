package cl.mastermartini.usuarios.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class InternalAuthenticationFilter
        extends OncePerRequestFilter {

    private final String internalKey;

    public InternalAuthenticationFilter(String internalKey) {

        if (internalKey == null || internalKey.isBlank()) {
            throw new IllegalArgumentException(
                    "La clave interna no puede estar vacia"
            );
        }

        this.internalKey = internalKey;
    }

    // Aplicar el filtro solamente al endpoint interno
    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        return !(
                "POST".equals(request.getMethod())
                        && "/api/usuarios/interno"
                        .equals(request.getServletPath())
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Obtener la clave enviada por auth-service
        String receivedKey = request.getHeader(
                "X-Internal-Key"
        );

        // Comprobar que la clave sea correcta
        if (receivedKey == null || !MessageDigest.isEqual(
                internalKey.getBytes(StandardCharsets.UTF_8),
                receivedKey.getBytes(StandardCharsets.UTF_8)
        )) {

            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Clave interna invalida"
            );

            return;
        }

        // Crear autenticacion exclusiva del servicio interno
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "auth-service",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_INTERNAL"
                                )
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
