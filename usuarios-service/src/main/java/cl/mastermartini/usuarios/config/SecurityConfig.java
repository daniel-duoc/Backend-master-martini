package cl.mastermartini.usuarios.config;

import cl.mastermartini.usuarios.security.InternalAuthenticationFilter;
import cl.mastermartini.usuarios.security.JwtAuthenticationFilter;
import cl.mastermartini.usuarios.security.JwtService;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final String internalKey;

    public SecurityConfig(
            JwtService jwtService,
            @Value("${internal.service.key}") String internalKey
    ) {

        this.jwtService = jwtService;
        this.internalKey = internalKey;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // Desactivar CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // Desactivar autenticacion tradicional
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // No utilizar sesiones de servidor
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Configurar respuestas de seguridad
                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint(
                                (request, response, error) ->
                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                "Autenticacion requerida"
                                        )
                        )

                        .accessDeniedHandler(
                                (request, response, error) ->
                                        response.sendError(
                                                HttpServletResponse.SC_FORBIDDEN,
                                                "Acceso denegado"
                                        )
                        )
                )

                // Configurar permisos por roles
                .authorizeHttpRequests(auth -> auth

                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        // Endpoint exclusivo para auth-service
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/usuarios/interno"
                        ).hasRole("INTERNAL")

                        // Consultar perfil: solo usuarios
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/usuarios/me"
                        ).hasRole("USER")

                        // Actualizar perfil: solo usuarios
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/usuarios/me"
                        ).hasRole("USER")

                        // Bloquear cualquier otra solicitud
                        .anyRequest().denyAll()
                )

                // Registrar filtro JWT
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class
                )

                // Registrar filtro de autenticacion interna
                .addFilterAfter(
                        new InternalAuthenticationFilter(internalKey),
                        JwtAuthenticationFilter.class
                );

        return http.build();
    }
}