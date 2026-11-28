package cl.mastermartini.auth.service;

import cl.mastermartini.auth.model.Credencial;
import cl.mastermartini.auth.model.EstadoRegistro;
import cl.mastermartini.auth.repository.CredencialRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistroTransaccionalService {

    private final CredencialRepository repository;

    public RegistroTransaccionalService(
            CredencialRepository repository
    ) {
        this.repository = repository;
    }

    // Guardar las credenciales como pendientes
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Credencial crearPendiente(
            String correo,
            String passwordHash,
            String nombre
    ) {

        if (repository.existsByCorreo(correo)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya esta registrado"
            );
        }

        Credencial credencial = new Credencial(
                correo,
                passwordHash,
                nombre
        );

        try {

            return repository.saveAndFlush(credencial);

        } catch (DataIntegrityViolationException e) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo crear el registro por un conflicto de datos",
                    e
            );
        }
    }

    // Activar la cuenta cuando el perfil fue creado
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Credencial activarRegistro(Long usuarioId) {

        Credencial credencial = repository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Credencial no encontrada"
                ));

        // Si ya esta activa, no repetir la operacion
        if (credencial.getEstadoRegistro()
                == EstadoRegistro.ACTIVO) {

            return credencial;
        }

        credencial.setEstadoRegistro(
                EstadoRegistro.ACTIVO
        );

        // El nombre principal pertenece a usuarios-service
        credencial.setNombreRegistro(null);

        return repository.save(credencial);
    }
}