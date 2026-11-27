
package cl.mastermartini.auth.controller;

import cl.mastermartini.auth.dto.AuthResponse;
import cl.mastermartini.auth.dto.LoginRequest;
import cl.mastermartini.auth.dto.RegistroRequest;
import cl.mastermartini.auth.service.AuthService;

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

    // Registrar una nueva cuenta
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registrar(
            @Valid @RequestBody RegistroRequest req) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(auth.registrar(req));
    }

    // Iniciar sesion
    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest req) {

        return auth.login(req);
    }
}

