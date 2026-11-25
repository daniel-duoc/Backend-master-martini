package cl.mastermartini.backend.controller;

import cl.mastermartini.backend.dto.AuthResponse;
import cl.mastermartini.backend.dto.LoginRequest;
import cl.mastermartini.backend.dto.RegistroRequest;
import cl.mastermartini.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.registrar(req));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }
}
