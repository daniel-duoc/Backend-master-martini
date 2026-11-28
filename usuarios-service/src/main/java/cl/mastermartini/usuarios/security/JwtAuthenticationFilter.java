package cl.mastermartini.usuarios.security;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {

        this.jwtService = jwtService;
    }

    // Excluir el endpoint de comunicacion interna
    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        return "POST".equals(request.getMethod())
                && "/api/usuarios/interno"
                .equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization =
                request.getHeader("Authorization");

        // Comprobar si existe un token Bearer
        if (authorization != null
                && authorization.regionMatches(
                true, 0, "Bearer ", 0, 7
        )) {

            String token = authorization.substring(7);

            Long usuarioId;

            try {

                // Validar el JWT una sola vez
                // y obtener el identificador
                usuarioId =
                        jwtService.obtenerUsuarioId(token);

            } catch (JwtException | IllegalArgumentException e) {

                SecurityContextHolder.clearContext();

                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Token invalido o expirado"
                );

                return;
            }

            // Crear autenticacion con el rol USER
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            usuarioId.toString(),
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_USER"
                                    )
                            )
                    );

            // Guardar la autenticacion
            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);
        }

        // Continuar con la solicitud
        filterChain.doFilter(request, response);
    }
}