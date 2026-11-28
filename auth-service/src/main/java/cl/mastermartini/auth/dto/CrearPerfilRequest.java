package cl.mastermartini.auth.dto;

public record CrearPerfilRequest(

        Long id,
        String nombre,
        String correo

) {
}