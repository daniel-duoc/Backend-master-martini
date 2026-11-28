package cl.mastermartini.auth.service;

import cl.mastermartini.auth.client.UsuariosClient;
import cl.mastermartini.auth.dto.AuthResponse;
import cl.mastermartini.auth.dto.LoginRequest;
import cl.mastermartini.auth.dto.RegistroRequest;
import cl.mastermartini.auth.dto.PerfilResponse;
import cl.mastermartini.auth.model.Credencial;
import cl.mastermartini.auth.model.EstadoRegistro;
import cl.mastermartini.auth.repository.CredencialRepository;
import cl.mastermartini.auth.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final CredencialRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final UsuariosClient usuariosClient;
    private final RegistroTransaccionalService registroTransaccional;

    public AuthService(
            CredencialRepository repository,
            PasswordEncoder encoder,
            JwtService jwtService,
            UsuariosClient usuariosClient,
            RegistroTransaccionalService registroTransaccional
    ) {
        this.repository = repository;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.usuariosClient = usuariosClient;
        this.registroTransaccional = registroTransaccional;
    }

    // Registrar un usuario o recuperar un registro pendiente
    public AuthResponse registrar(RegistroRequest request) {

        String correo = request.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        String nombre = request.nombre().trim();

        // Buscar credenciales existentes
        Credencial credencial = repository.findByCorreo(correo)
                .orElse(null);

        if (credencial != null) {

            // Validar si el registro pendiente puede recuperarse
            validarRegistroPendiente(
                    credencial,
                    request.password()
            );

        } else {

            try {

                // Crear credenciales pendientes
                credencial = registroTransaccional.crearPendiente(
                        correo,
                        encoder.encode(request.password()),
                        nombre
                );

            } catch (ResponseStatusException e) {

                // Propagar errores que no sean conflictos
                if (e.getStatusCode().value() != 409) {
                    throw e;
                }

                // Recuperar una posible insercion concurrente
                credencial = repository.findByCorreo(correo)
                        .orElseThrow(() -> e);

                validarRegistroPendiente(
                        credencial,
                        request.password()
                );
            }
        }

        // Recuperar el nombre original del registro
        nombre = credencial.getNombreRegistro();

        // Crear o recuperar el perfil sin duplicarlo
        usuariosClient.crearPerfil(
                credencial.getId(),
                nombre,
                correo
        );

        // Activar la cuenta despues de confirmar el perfil
        registroTransaccional.activarRegistro(
                credencial.getId()
        );

        // Generar JWT despues de confirmar la activacion
        String token = jwtService.generarToken(
                credencial.getId(),
                correo
        );

        return new AuthResponse(
                token,
                nombre,
                correo
        );
    }

    // Validar que un registro pendiente pueda recuperarse
    private void validarRegistroPendiente(
            Credencial credencial,
            String password
    ) {

        // Rechazar cuentas que ya fueron activadas
        if (credencial.getEstadoRegistro()
                != EstadoRegistro.PENDIENTE) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya esta registrado"
            );
        }

        // Verificar la contrasena original
        if (!encoder.matches(
                password,
                credencial.getPasswordHash()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo completar el registro"
            );
        }

        // Comprobar que existan los datos pendientes
        if (credencial.getNombreRegistro() == null
                || credencial.getNombreRegistro().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El registro pendiente no contiene los datos necesarios"
            );
        }
    }

    // Iniciar sesion
    public AuthResponse login(LoginRequest request) {

        String correo = request.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Buscar las credenciales del usuario
        Credencial credencial = repository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Credenciales incorrectas"
                ));

        // Verificar la contrasena
        if (!encoder.matches(
                request.password(),
                credencial.getPasswordHash()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Credenciales incorrectas"
            );
        }

        // Impedir el acceso a cuentas pendientes
        if (credencial.getEstadoRegistro()
                != EstadoRegistro.ACTIVO) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El registro del usuario esta pendiente"
            );
        }

        // Generar token JWT
        String token = jwtService.generarToken(
                credencial.getId(),
                correo
        );

        // Consultar el perfil en usuarios-service
        PerfilResponse perfil =
                usuariosClient.obtenerPerfil(token);

        // Comprobar que el perfil corresponde al usuario
        if (!credencial.getId().equals(perfil.id())
                || !correo.equals(perfil.correo())) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Los datos del perfil no coinciden"
            );
        }

        return new AuthResponse(
                token,
                perfil.nombre(),
                correo
        );
    }
}
