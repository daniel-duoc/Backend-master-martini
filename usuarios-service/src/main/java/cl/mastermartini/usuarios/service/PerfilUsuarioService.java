package cl.mastermartini.usuarios.service;

import cl.mastermartini.usuarios.dto.ActualizarPerfilRequest;
import cl.mastermartini.usuarios.dto.CrearPerfilRequest;
import cl.mastermartini.usuarios.dto.PerfilResponse;
import cl.mastermartini.usuarios.model.PerfilUsuario;
import cl.mastermartini.usuarios.repository.PerfilUsuarioRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class PerfilUsuarioService {

    private final PerfilUsuarioRepository repository;
    private final PerfilTransaccionalService perfilTransaccional;

    public PerfilUsuarioService(
            PerfilUsuarioRepository repository,
            PerfilTransaccionalService perfilTransaccional
    ) {
        this.repository = repository;
        this.perfilTransaccional = perfilTransaccional;
    }

    // Crear o recuperar un perfil de usuario
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PerfilResponse crearPerfil(CrearPerfilRequest request) {

        String correo = request.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        String nombre = request.nombre().trim();

        // Buscar un perfil con el mismo identificador
        PerfilUsuario existente = repository.findById(request.id())
                .orElse(null);

        if (existente != null) {

            return validarPerfilExistente(existente, correo);
        }

        // Verificar que el correo no pertenezca a otro perfil
        if (repository.existsByCorreo(correo)) {

            // Consultar nuevamente por si hubo una insercion simultanea
            PerfilUsuario recuperado = repository.findById(request.id())
                    .orElse(null);

            if (recuperado != null) {
                return validarPerfilExistente(recuperado, correo);
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya esta registrado"
            );
        }

        try {

            // Insertar mediante una transaccion independiente
            PerfilUsuario guardado =
                    perfilTransaccional.insertarPerfil(
                            request.id(),
                            nombre,
                            correo
                    );

            return convertirResponse(guardado);

        } catch (DataIntegrityViolationException e) {

            // Recuperar el perfil despues de revertir
            // la transaccion de insercion fallida
            PerfilUsuario recuperado = repository.findById(request.id())
                    .orElse(null);

            if (recuperado != null) {

                return validarPerfilExistente(
                        recuperado,
                        correo
                );
            }

            // Comprobar si el correo pertenece a otro usuario
            if (repository.existsByCorreo(correo)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El correo ya esta registrado",
                        e
                );
            }

            // No ocultar otros errores de integridad
            throw e;
        }
    }

    // Validar que el perfil existente corresponda al mismo usuario
    private PerfilResponse validarPerfilExistente(
            PerfilUsuario perfil,
            String correo
    ) {

        if (correo.equals(perfil.getCorreo())) {

            // Devolver el perfil sin modificar sus datos
            return convertirResponse(perfil);
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El identificador pertenece a otro usuario"
        );
    }

    // Obtener el perfil mediante el identificador
    @Transactional(readOnly = true)
    public PerfilResponse obtenerPerfil(Long usuarioId) {

        PerfilUsuario perfil = buscarPerfil(usuarioId);

        return convertirResponse(perfil);
    }

    // Actualizar los datos personales y empresariales
    @Transactional
    public PerfilResponse actualizarPerfil(
            Long usuarioId,
            ActualizarPerfilRequest request
    ) {

        PerfilUsuario perfil = buscarPerfil(usuarioId);

        perfil.setNombre(request.nombre().trim());
        perfil.setTelefono(request.telefono());
        perfil.setTipoNegocio(request.tipoNegocio());
        perfil.setIntereses(request.intereses());
        perfil.setFoto(request.foto());
        perfil.setNombreEmpresa(request.nombreEmpresa());
        perfil.setDireccionEmpresa(request.direccionEmpresa());
        perfil.setCargoEmpresa(request.cargoEmpresa());
        perfil.setDescripcionEmpresa(request.descripcionEmpresa());

        PerfilUsuario actualizado = repository.save(perfil);

        return convertirResponse(actualizado);
    }

    // Metodo auxiliar para buscar un perfil
    private PerfilUsuario buscarPerfil(Long usuarioId) {

        return repository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Perfil de usuario no encontrado"
                ));
    }

    // Convertir la entidad en un DTO de respuesta
    private PerfilResponse convertirResponse(PerfilUsuario perfil) {

        return new PerfilResponse(
                perfil.getId(),
                perfil.getNombre(),
                perfil.getCorreo(),
                perfil.getTelefono(),
                perfil.getTipoNegocio(),
                perfil.getIntereses(),
                perfil.getFoto(),
                perfil.getNombreEmpresa(),
                perfil.getDireccionEmpresa(),
                perfil.getCargoEmpresa(),
                perfil.getDescripcionEmpresa()
        );
    }
}