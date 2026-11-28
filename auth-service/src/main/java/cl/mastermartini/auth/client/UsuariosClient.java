package cl.mastermartini.auth.client;

import cl.mastermartini.auth.dto.CrearPerfilRequest;
import cl.mastermartini.auth.dto.PerfilResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UsuariosClient {

    private static final Logger logger = LoggerFactory.getLogger(UsuariosClient.class);

    private final RestClient restClient;
    private final String internalKey;

    public UsuariosClient(
            @Value("${usuarios.service.url}") String usuariosUrl,
            @Value("${internal.service.key}") String internalKey
    ) {

        if (internalKey == null || internalKey.isBlank()) {
            throw new IllegalArgumentException(
                    "La clave interna no puede estar vacia"
            );
        }

        this.internalKey = internalKey;

        // Configurar tiempos maximos de espera
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        // Configurar cliente HTTP
        this.restClient = RestClient.builder()
                .baseUrl(usuariosUrl)
                .requestFactory(factory)
                .build();
    }

    // Solicitar la creacion de un perfil
    public void crearPerfil(
            Long id,
            String nombre,
            String correo
    ) {

        CrearPerfilRequest request =
                new CrearPerfilRequest(id, nombre, correo);

        try {

            restClient.post()
                    .uri("/api/usuarios/interno")
                    .header("X-Internal-Key", internalKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException e) {

            // Diagnosticar el error de comunicacion
            if (e instanceof RestClientResponseException respuesta) {

                logger.warn("Error HTTP usuarios-service. Estado: {}", respuesta.getStatusCode().value());

            } else {

                logger.warn("Error de comunicacion con usuarios-service. Tipo: {}", e.getClass().getSimpleName());
            }

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo crear el perfil del usuario",
                    e
            );
        }
    }

    // Consultar el perfil mediante JWT
    public PerfilResponse obtenerPerfil(String token) {

        try {

            PerfilResponse perfil = restClient.get()
                    .uri("/api/usuarios/me")
                    .headers(headers ->
                            headers.setBearerAuth(token))
                    .retrieve()
                    .body(PerfilResponse.class);

            if (perfil == null
                    || perfil.nombre() == null
                    || perfil.nombre().isBlank()) {

                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo recuperar el perfil"
                );
            }

            return perfil;

        } catch (RestClientException e) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo consultar el perfil del usuario",
                    e
            );
        }
    }
}