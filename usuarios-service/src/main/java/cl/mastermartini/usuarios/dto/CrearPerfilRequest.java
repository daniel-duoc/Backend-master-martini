package cl.mastermartini.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CrearPerfilRequest(

        @NotNull
        @Positive
        Long id,

        @NotBlank
        @Size(max = 255)
        String nombre,

        @NotBlank
        @Email
        @Size(max = 255)
        String correo

) {
}
