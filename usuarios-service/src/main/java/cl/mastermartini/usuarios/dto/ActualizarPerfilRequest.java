package cl.mastermartini.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarPerfilRequest(

        @NotBlank
        @Size(max = 255)
        String nombre,

        @Size(max = 255)
        String telefono,

        @Size(max = 255)
        String tipoNegocio,

        @Size(max = 1000)
        String intereses,

        @Size(max = 2048)
        String foto,

        @Size(max = 255)
        String nombreEmpresa,

        @Size(max = 255)
        String direccionEmpresa,

        @Size(max = 255)
        String cargoEmpresa,

        @Size(max = 2000)
        String descripcionEmpresa

) {
}
