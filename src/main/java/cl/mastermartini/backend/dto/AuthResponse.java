package cl.mastermartini.backend.dto;

public record AuthResponse(String token, String nombre, String correo) {
}
