package cl.mastermartini.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PerfilResponse(
        Long id,
        String nombre,
        String correo
) {
}