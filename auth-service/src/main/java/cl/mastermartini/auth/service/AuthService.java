package cl.mastermartini.auth.service;

import cl.mastermartini.auth.dto.AuthResponse;
import cl.mastermartini.auth.dto.LoginRequest;
import cl.mastermartini.auth.dto.RegistroRequest;
import cl.mastermartini.auth.model.Credencial;
import cl.mastermartini.auth.repository.CredencialRepository;
import cl.mastermartini.auth.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final CredencialRepository credenciales;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(
            CredencialRepository credenciales,
            PasswordEncoder encoder,
            JwtService jwt) {

        this.credenciales = credenciales;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    // Registrar una nueva cuenta
    public AuthResponse registrar(RegistroRequest req) {

        String correo = req.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Comprobar si el correo ya existe
        if (credenciales.existsByCorreo(correo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una cuenta con ese correo");
        }

        // Guardar las credenciales con la contraseña protegida
        Credencial credencial = credenciales.save(
                new Credencial(
                        correo,
                        encoder.encode(req.password())
                )
        );

        // Generar el token JWT
        String token = jwt.generarToken(
                credencial.getId(),
                credencial.getCorreo()
        );

        return new AuthResponse(
                token,
                req.nombre().trim(),
                credencial.getCorreo()
        );
    }

    // Iniciar sesión
    public AuthResponse login(LoginRequest req) {

        String correo = req.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Buscar y validar las credenciales
        Credencial credencial = credenciales
                .findByCorreo(correo)
                .filter(c -> encoder.matches(
                        req.password(),
                        c.getPasswordHash()))
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Correo o contraseña incorrectos"
                        )
                );

        // Generar el token JWT
        String token = jwt.generarToken(
                credencial.getId(),
                credencial.getCorreo()
        );

        return new AuthResponse(
                token,
                "",
                credencial.getCorreo()
        );
    }
}
