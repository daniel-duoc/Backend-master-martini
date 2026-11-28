package cl.mastermartini.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public record LoginRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        @Size(max = 255, message = "El correo no puede superar los 255 caracteres")
        String correo,

        @NotBlank(message = "La contrasena es obligatoria")
        String password

) {

        // Normalizar el correo antes de su validacion
        public LoginRequest {

                if (correo != null) {
                        correo = correo.trim()
                                .toLowerCase(Locale.ROOT);
                }
        }

        // Validar el limite de bytes permitido por BCrypt
        @AssertTrue(message = "La contrasena no puede superar los 72 bytes")
        public boolean isPasswordLongitudValida() {

                if (password == null) {
                        return true;
                }

                return password.getBytes(
                        StandardCharsets.UTF_8
                ).length <= 72;
        }
}