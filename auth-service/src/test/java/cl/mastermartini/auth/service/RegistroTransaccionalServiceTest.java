package cl.mastermartini.auth.service;

import cl.mastermartini.auth.model.Credencial;
import cl.mastermartini.auth.model.EstadoRegistro;
import cl.mastermartini.auth.repository.CredencialRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistroTransaccionalServiceTest {

    private CredencialRepository repository;
    private RegistroTransaccionalService service;

    private static final Long ID = 1L;
    private static final String CORREO = "prueba@example.com";
    private static final String NOMBRE = "Usuario Prueba";
    private static final String HASH = "password-protegida";

    @BeforeEach
    void configurar() {

        repository = mock(CredencialRepository.class);

        service = new RegistroTransaccionalService(
                repository
        );
    }

    // Metodo auxiliar para crear una credencial pendiente

    private Credencial credencialPendiente() {

        Credencial credencial = new Credencial(
                CORREO,
                HASH,
                NOMBRE
        );

        credencial.setId(ID);

        credencial.setEstadoRegistro(
                EstadoRegistro.PENDIENTE
        );

        return credencial;
    }

    // PU-TRANS-01: Crear credencial pendiente correctamente
    @Test
    void crearCredencialPendiente() {

        when(repository.existsByCorreo(CORREO))
                .thenReturn(false);

        when(repository.saveAndFlush(any(Credencial.class)))
                .thenAnswer(invocation -> {

                    Credencial credencial =
                            invocation.getArgument(0);

                    credencial.setId(ID);

                    return credencial;
                });

        Credencial resultado = service.crearPendiente(
                CORREO,
                HASH,
                NOMBRE
        );

        assertEquals(ID, resultado.getId());
        assertEquals(CORREO, resultado.getCorreo());
        assertEquals(HASH, resultado.getPasswordHash());
        assertEquals(NOMBRE, resultado.getNombreRegistro());

        assertEquals(
                EstadoRegistro.PENDIENTE,
                resultado.getEstadoRegistro()
        );

        verify(repository, times(1))
                .saveAndFlush(any(Credencial.class));
    }

    // PU-TRANS-02: Rechazar correo duplicado
    @Test
    void rechazarCorreoDuplicado() {

        when(repository.existsByCorreo(CORREO))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.crearPendiente(
                        CORREO,
                        HASH,
                        NOMBRE
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(repository, never())
                .saveAndFlush(any(Credencial.class));
    }

    // PU-TRANS-03: Gestionar conflicto de integridad
    @Test
    void gestionarConflictoDeIntegridad() {

        when(repository.existsByCorreo(CORREO))
                .thenReturn(false);

        when(repository.saveAndFlush(any(Credencial.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Conflicto de datos"
                ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.crearPendiente(
                        CORREO,
                        HASH,
                        NOMBRE
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(repository, times(1))
                .saveAndFlush(any(Credencial.class));
    }

    // PU-TRANS-04: Activar credencial pendiente
    @Test
    void activarCredencialPendiente() {

        Credencial credencial = credencialPendiente();

        when(repository.findById(ID))
                .thenReturn(Optional.of(credencial));

        when(repository.save(any(Credencial.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        Credencial resultado =
                service.activarRegistro(ID);

        assertEquals(
                EstadoRegistro.ACTIVO,
                resultado.getEstadoRegistro()
        );

        assertEquals(ID, resultado.getId());
        assertEquals(CORREO, resultado.getCorreo());
        assertEquals(HASH, resultado.getPasswordHash());

        assertNull(resultado.getNombreRegistro());

        verify(repository, times(1))
                .save(credencial);
    }

    // PU-TRANS-05: Activacion idempotente
    @Test
    void activarCredencialYaActiva() {

        Credencial credencial = credencialPendiente();

        credencial.setEstadoRegistro(
                EstadoRegistro.ACTIVO
        );

        credencial.setNombreRegistro(null);

        when(repository.findById(ID))
                .thenReturn(Optional.of(credencial));

        Credencial resultado =
                service.activarRegistro(ID);

        assertSame(credencial, resultado);

        assertEquals(
                EstadoRegistro.ACTIVO,
                resultado.getEstadoRegistro()
        );

        verify(repository, never())
                .save(any(Credencial.class));
    }

    // PU-TRANS-06: Activar credencial inexistente
    @Test
    void rechazarActivacionDeCredencialInexistente() {

        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.activarRegistro(99L)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(repository, never())
                .save(any(Credencial.class));
    }

    // PU-TRANS-07: Propagar fallo durante la activacion
    @Test
    void propagarFalloDuranteActivacion() {

        Credencial credencial = credencialPendiente();

        when(repository.findById(ID))
                .thenReturn(Optional.of(credencial));

        DataIntegrityViolationException error =
                new DataIntegrityViolationException(
                        "No se pudo guardar la activacion"
                );

        when(repository.save(any(Credencial.class)))
                .thenThrow(error);

        DataIntegrityViolationException exception =
                assertThrows(
                        DataIntegrityViolationException.class,
                        () -> service.activarRegistro(ID)
                );

        assertSame(error, exception);

        verify(repository, times(1))
                .save(credencial);
    }
}
