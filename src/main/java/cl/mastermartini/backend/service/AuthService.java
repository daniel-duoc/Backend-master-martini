package cl.mastermartini.backend.service;

import cl.mastermartini.backend.dto.AuthResponse;
import cl.mastermartini.backend.dto.LoginRequest;
import cl.mastermartini.backend.dto.RegistroRequest;
import cl.mastermartini.backend.model.Usuario;
import cl.mastermartini.backend.repository.UsuarioRepository;
import cl.mastermartini.backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse registrar(RegistroRequest req) {
        String correo = req.correo().trim().toLowerCase();

        if (usuarios.existsByCorreo(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo");
        }

        Usuario usuario = usuarios.save(
                new Usuario(req.nombre().trim(), correo, encoder.encode(req.password())));

        return new AuthResponse(jwt.generarToken(usuario.getCorreo()), usuario.getNombre(), usuario.getCorreo());
    }

    public AuthResponse login(LoginRequest req) {
        String correo = req.correo().trim().toLowerCase();

        Usuario usuario = usuarios.findByCorreo(correo)
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));

        return new AuthResponse(jwt.generarToken(usuario.getCorreo()), usuario.getNombre(), usuario.getCorreo());
    }
}
