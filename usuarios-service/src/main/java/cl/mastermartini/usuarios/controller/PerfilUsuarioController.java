package cl.mastermartini.usuarios.controller;

import cl.mastermartini.usuarios.dto.ActualizarPerfilRequest;
import cl.mastermartini.usuarios.dto.CrearPerfilRequest;
import cl.mastermartini.usuarios.dto.PerfilResponse;
import cl.mastermartini.usuarios.service.PerfilUsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class PerfilUsuarioController {

    private final PerfilUsuarioService service;

    public PerfilUsuarioController(PerfilUsuarioService service) {
        this.service = service;
    }

    // Crear un perfil desde auth-service
    @PostMapping("/interno")
    public ResponseEntity<PerfilResponse> crearPerfil(
            @Valid @RequestBody CrearPerfilRequest request
    ) {

        PerfilResponse perfil = service.crearPerfil(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(perfil);
    }

    // Obtener el perfil del usuario autenticado
    @GetMapping("/me")
    public ResponseEntity<PerfilResponse> obtenerMiPerfil(
            Authentication authentication
    ) {

        Long usuarioId = Long.valueOf(authentication.getName());

        PerfilResponse perfil = service.obtenerPerfil(usuarioId);

        return ResponseEntity.ok(perfil);
    }

    // Actualizar el perfil del usuario autenticado
    @PutMapping("/me")
    public ResponseEntity<PerfilResponse> actualizarMiPerfil(
            Authentication authentication,
            @Valid @RequestBody ActualizarPerfilRequest request
    ) {

        Long usuarioId = Long.valueOf(authentication.getName());

        PerfilResponse perfil = service.actualizarPerfil(
                usuarioId,
                request
        );

        return ResponseEntity.ok(perfil);
    }
}
