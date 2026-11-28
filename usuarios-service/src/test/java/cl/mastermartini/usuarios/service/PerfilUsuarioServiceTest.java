package cl.mastermartini.usuarios.service;

import cl.mastermartini.usuarios.dto.ActualizarPerfilRequest;
import cl.mastermartini.usuarios.dto.CrearPerfilRequest;
import cl.mastermartini.usuarios.dto.PerfilResponse;
import cl.mastermartini.usuarios.model.PerfilUsuario;
import cl.mastermartini.usuarios.repository.PerfilUsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PerfilUsuarioServiceTest {

    private PerfilUsuarioRepository repository;
    private PerfilTransaccionalService perfilTransaccional;
    private PerfilUsuarioService service;

    private static final Long ID = 1L;
    private static final String NOMBRE = "Usuario Prueba";
    private static final String CORREO = "prueba@example.com";

    @BeforeEach
    void configurar() {

        repository = mock(PerfilUsuarioRepository.class);

        perfilTransaccional =
                mock(PerfilTransaccionalService.class);

        service = new PerfilUsuarioService(
                repository,
                perfilTransaccional
        );
    }

    // Metodos auxiliares

    private PerfilUsuario perfilPrueba() {

        return new PerfilUsuario(
                ID,
                NOMBRE,
                CORREO
        );
    }

    private CrearPerfilRequest solicitudCreacion() {

        return new CrearPerfilRequest(
                ID,
                NOMBRE,
                CORREO
        );
    }

    private ActualizarPerfilRequest solicitudActualizacion() {

        return new ActualizarPerfilRequest(
                "Usuario Actualizado",
                "912345678",
                "Pasteleria",
                "Chocolate y reposteria",
                null,
                "Pasteleria Prueba",
                "Santiago",
                "Administrador",
                "Empresa de pasteleria"
        );
    }

    private void prepararPerfilNuevo() {

        when(repository.findById(ID))
                .thenReturn(Optional.empty());

        when(repository.existsByCorreo(CORREO))
                .thenReturn(false);

        when(perfilTransaccional.insertarPerfil(
                ID, NOMBRE, CORREO))
                .thenReturn(perfilPrueba());
    }

    // PU-USU-01: Creacion correcta del perfil
    @Test
    void crearPerfilCorrectamente() {

        prepararPerfilNuevo();

        PerfilResponse response =
                service.crearPerfil(solicitudCreacion());

        assertEquals(ID, response.id());
        assertEquals(NOMBRE, response.nombre());
        assertEquals(CORREO, response.correo());

        verify(perfilTransaccional, times(1))
                .insertarPerfil(ID, NOMBRE, CORREO);

        verify(repository, never())
                .save(any(PerfilUsuario.class));
    }

    // PU-USU-02: Creacion idempotente
    @Test
    void recuperarPerfilExistente() {

        when(repository.findById(ID))
                .thenReturn(Optional.of(perfilPrueba()));

        PerfilResponse response =
                service.crearPerfil(solicitudCreacion());

        assertEquals(ID, response.id());
        assertEquals(NOMBRE, response.nombre());
        assertEquals(CORREO, response.correo());

        verifyNoInteractions(perfilTransaccional);

        verify(repository, never())
                .save(any(PerfilUsuario.class));
    }

    // PU-USU-03: Correo duplicado con otro ID
    @Test
    void rechazarCorreoDuplicado() {

        CrearPerfilRequest request = new CrearPerfilRequest(
                2L,
                NOMBRE,
                CORREO
        );

        when(repository.findById(2L))
                .thenReturn(Optional.empty());

        when(repository.existsByCorreo(CORREO))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.crearPerfil(request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verifyNoInteractions(perfilTransaccional);

        verify(repository, never())
                .save(any(PerfilUsuario.class));
    }

    // PU-USU-04: Consultar perfil existente
    @Test
    void obtenerPerfilExistente() {

        when(repository.findById(ID))
                .thenReturn(Optional.of(perfilPrueba()));

        PerfilResponse response =
                service.obtenerPerfil(ID);

        assertEquals(ID, response.id());
        assertEquals(NOMBRE, response.nombre());
        assertEquals(CORREO, response.correo());

        verifyNoInteractions(perfilTransaccional);
    }

    // PU-USU-05: Consultar perfil inexistente
    @Test
    void obtenerPerfilInexistente() {

        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.obtenerPerfil(99L)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
    }

    // PU-USU-06: Actualizar perfil correctamente
    @Test
    void actualizarPerfilCorrectamente() {

        when(repository.findById(ID))
                .thenReturn(Optional.of(perfilPrueba()));

        when(repository.save(any(PerfilUsuario.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        PerfilResponse response = service.actualizarPerfil(
                ID,
                solicitudActualizacion()
        );

        assertEquals(ID, response.id());
        assertEquals("Usuario Actualizado", response.nombre());
        assertEquals(CORREO, response.correo());
        assertEquals("912345678", response.telefono());
        assertEquals("Pasteleria", response.tipoNegocio());

        assertEquals(
                "Chocolate y reposteria",
                response.intereses()
        );

        assertNull(response.foto());

        assertEquals(
                "Pasteleria Prueba",
                response.nombreEmpresa()
        );

        assertEquals(
                "Santiago",
                response.direccionEmpresa()
        );

        assertEquals(
                "Administrador",
                response.cargoEmpresa()
        );

        assertEquals(
                "Empresa de pasteleria",
                response.descripcionEmpresa()
        );

        verify(repository, times(1))
                .save(any(PerfilUsuario.class));

        verifyNoInteractions(perfilTransaccional);
    }

    // PU-USU-07: Actualizar perfil inexistente
    @Test
    void actualizarPerfilInexistente() {

        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.actualizarPerfil(
                        99L,
                        solicitudActualizacion()
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(repository, never())
                .save(any(PerfilUsuario.class));

        verifyNoInteractions(perfilTransaccional);
    }

    // PU-USU-08: Mismo ID con correo diferente
    @Test
    void rechazarIdConCorreoDiferente() {

        when(repository.findById(ID))
                .thenReturn(Optional.of(perfilPrueba()));

        CrearPerfilRequest request = new CrearPerfilRequest(
                ID,
                "Otro Usuario",
                "otro@example.com"
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.crearPerfil(request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verifyNoInteractions(perfilTransaccional);

        verify(repository, never())
                .save(any(PerfilUsuario.class));
    }

    // PU-USU-09: Conservar nombre original
    @Test
    void conservarNombreOriginal() {

        when(repository.findById(ID))
                .thenReturn(Optional.of(perfilPrueba()));

        CrearPerfilRequest request = new CrearPerfilRequest(
                ID,
                "Nombre Diferente",
                CORREO
        );

        PerfilResponse response =
                service.crearPerfil(request);

        assertEquals(ID, response.id());
        assertEquals(NOMBRE, response.nombre());
        assertEquals(CORREO, response.correo());

        verifyNoInteractions(perfilTransaccional);

        verify(repository, never())
                .save(any(PerfilUsuario.class));
    }

    // PU-USU-10: Normalizacion de nombre y correo
    @Test
    void normalizarDatosDelPerfil() {

        prepararPerfilNuevo();

        CrearPerfilRequest request = new CrearPerfilRequest(
                ID,
                " Usuario Prueba ",
                " PRUEBA@EXAMPLE.COM "
        );

        PerfilResponse response =
                service.crearPerfil(request);

        assertEquals(NOMBRE, response.nombre());
        assertEquals(CORREO, response.correo());

        verify(repository).existsByCorreo(CORREO);

        verify(perfilTransaccional)
                .insertarPerfil(ID, NOMBRE, CORREO);
    }

    // PU-USU-11: Recuperar insercion concurrente compatible
    @Test
    void recuperarInsercionConcurrente() {

        when(repository.findById(ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(perfilPrueba()));

        when(repository.existsByCorreo(CORREO))
                .thenReturn(false);

        when(perfilTransaccional.insertarPerfil(
                ID, NOMBRE, CORREO))
                .thenThrow(new DataIntegrityViolationException(
                        "Conflicto de insercion"
                ));

        PerfilResponse response =
                service.crearPerfil(solicitudCreacion());

        assertEquals(ID, response.id());
        assertEquals(NOMBRE, response.nombre());
        assertEquals(CORREO, response.correo());

        verify(repository, times(2))
                .findById(ID);

        verify(perfilTransaccional, times(1))
                .insertarPerfil(ID, NOMBRE, CORREO);

        verify(repository, never())
                .save(any(PerfilUsuario.class));
    }

    // PU-USU-12: Insercion concurrente con ID incompatible
    @Test
    void rechazarInsercionConcurrenteConOtroCorreo() {

        PerfilUsuario otroPerfil = new PerfilUsuario(
                ID,
                "Otro Usuario",
                "otro@example.com"
        );

        when(repository.findById(ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(otroPerfil));

        when(repository.existsByCorreo(CORREO))
                .thenReturn(false);

        when(perfilTransaccional.insertarPerfil(
                ID, NOMBRE, CORREO))
                .thenThrow(new DataIntegrityViolationException(
                        "Conflicto de identificador"
                ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.crearPerfil(solicitudCreacion())
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(repository, never())
                .save(any(PerfilUsuario.class));
    }

    // PU-USU-13: Insercion concurrente con correo duplicado
    @Test
    void rechazarInsercionConcurrenteConCorreoDuplicado() {

        when(repository.findById(ID))
                .thenReturn(Optional.empty());

        when(repository.existsByCorreo(CORREO))
                .thenReturn(false)
                .thenReturn(true);

        when(perfilTransaccional.insertarPerfil(
                ID, NOMBRE, CORREO))
                .thenThrow(new DataIntegrityViolationException(
                        "Correo duplicado"
                ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.crearPerfil(solicitudCreacion())
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(repository, times(2))
                .existsByCorreo(CORREO);
    }

    // PU-USU-14: Perfil creado durante la comprobacion del correo
    @Test
    void recuperarPerfilCreadoDuranteComprobacion() {

        when(repository.findById(ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(perfilPrueba()));

        when(repository.existsByCorreo(CORREO))
                .thenReturn(true);

        PerfilResponse response =
                service.crearPerfil(solicitudCreacion());

        assertEquals(ID, response.id());
        assertEquals(NOMBRE, response.nombre());
        assertEquals(CORREO, response.correo());

        verify(repository, times(2))
                .findById(ID);

        verifyNoInteractions(perfilTransaccional);
    }

    // PU-USU-15: Conservar modificaciones de un perfil
    @Test
    void conservarPerfilActualizadoDuranteReintento() {

        PerfilUsuario perfil = perfilPrueba();

        perfil.setNombre("Usuario Actualizado");
        perfil.setTelefono("912345678");
        perfil.setTipoNegocio("Pasteleria");
        perfil.setIntereses("Chocolateria");
        perfil.setFoto("https://example.com/foto.png");
        perfil.setNombreEmpresa("Pasteleria Prueba");
        perfil.setDireccionEmpresa("Santiago");
        perfil.setCargoEmpresa("Administrador");
        perfil.setDescripcionEmpresa("Empresa actualizada");

        when(repository.findById(ID))
                .thenReturn(Optional.of(perfil));

        PerfilResponse response =
                service.crearPerfil(solicitudCreacion());

        assertEquals(ID, response.id());
        assertEquals(CORREO, response.correo());
        assertEquals("Usuario Actualizado", response.nombre());
        assertEquals("912345678", response.telefono());
        assertEquals("Pasteleria", response.tipoNegocio());
        assertEquals("Chocolateria", response.intereses());

        assertEquals(
                "https://example.com/foto.png",
                response.foto()
        );

        assertEquals(
                "Pasteleria Prueba",
                response.nombreEmpresa()
        );

        assertEquals(
                "Santiago",
                response.direccionEmpresa()
        );

        assertEquals(
                "Administrador",
                response.cargoEmpresa()
        );

        assertEquals(
                "Empresa actualizada",
                response.descripcionEmpresa()
        );

        verifyNoInteractions(perfilTransaccional);

        verify(repository, never())
                .save(any(PerfilUsuario.class));
    }

    // PU-USU-16: Propagar errores de integridad desconocidos
    @Test
    void propagarErrorDeIntegridadDesconocido() {

        when(repository.findById(ID))
                .thenReturn(Optional.empty());

        when(repository.existsByCorreo(CORREO))
                .thenReturn(false);

        DataIntegrityViolationException error =
                new DataIntegrityViolationException(
                        "Error de integridad desconocido"
                );

        when(perfilTransaccional.insertarPerfil(
                ID, NOMBRE, CORREO))
                .thenThrow(error);

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> service.crearPerfil(solicitudCreacion())
        );

        assertSame(error, exception);

        verify(repository, times(2))
                .findById(ID);

        verify(repository, times(2))
                .existsByCorreo(CORREO);
    }
}