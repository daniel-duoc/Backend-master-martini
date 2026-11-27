package cl.mastermartini.auth.dto;

public record AuthResponse(
        String token,
        String nombre,
        String correo
) {
}
