package cl.mastermartini.auth.service;

import cl.mastermartini.auth.model.Credencial;
import cl.mastermartini.auth.model.EstadoRegistro;
import cl.mastermartini.auth.repository.CredencialRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@Import(RegistroTransaccionalService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RegistroTransaccionalIntegrationTest {

    @Autowired
    private CredencialRepository repository;

    @Autowired
    private RegistroTransaccionalService service;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static final String CORREO = "prueba@example.com";
    private static final String NOMBRE = "Usuario Prueba";
    private static final String HASH = "password-protegida";

    @BeforeEach
    void limpiarBaseDeDatos() {

        repository.deleteAll();
    }

    // Metodo auxiliar

    private Credencial crearCredencialPendiente() {

        return service.crearPendiente(
                CORREO,
                HASH,
                NOMBRE
        );
    }

    // PI-AUTH-01: Persistencia real de credencial pendiente
    @Test
    void persistirCredencialPendiente() {

        Credencial creada = crearCredencialPendiente();

        assertNotNull(creada.getId());

        Credencial guardada = repository
                .findById(creada.getId())
                .orElseThrow();

        assertEquals(creada.getId(), guardada.getId());
        assertEquals(CORREO, guardada.getCorreo());
        assertEquals(HASH, guardada.getPasswordHash());
        assertEquals(NOMBRE, guardada.getNombreRegistro());

        assertEquals(
                EstadoRegistro.PENDIENTE,
                guardada.getEstadoRegistro()
        );

        assertEquals(1L, repository.count());
    }

    // PI-AUTH-02: Activacion real de credencial
    @Test
    void activarCredencialCorrectamente() {

        Credencial creada = crearCredencialPendiente();

        service.activarRegistro(creada.getId());

        Credencial guardada = repository
                .findById(creada.getId())
                .orElseThrow();

        assertEquals(
                EstadoRegistro.ACTIVO,
                guardada.getEstadoRegistro()
        );

        assertEquals(CORREO, guardada.getCorreo());
        assertEquals(HASH, guardada.getPasswordHash());

        assertNull(guardada.getNombreRegistro());

        assertEquals(1L, repository.count());
    }

    // PI-AUTH-03: Restriccion real de correo duplicado
    @Test
    void rechazarCorreoDuplicado() {

        Credencial original = crearCredencialPendiente();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.crearPendiente(
                        CORREO,
                        "otro-hash",
                        "Otro Usuario"
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        // Comprobar tambien la restriccion de la base de datos

        Credencial duplicada = new Credencial(
                CORREO,
                "otro-hash",
                "Otro Usuario"
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(duplicada)
        );

        assertEquals(1L, repository.count());

        Credencial guardada = repository
                .findByCorreo(CORREO)
                .orElseThrow();

        assertEquals(original.getId(), guardada.getId());
        assertEquals(HASH, guardada.getPasswordHash());
        assertEquals(NOMBRE, guardada.getNombreRegistro());
    }

    // PI-AUTH-04: Activacion idempotente
    @Test
    void activarCredencialVariasVeces() {

        Credencial creada = crearCredencialPendiente();

        service.activarRegistro(creada.getId());
        service.activarRegistro(creada.getId());
        service.activarRegistro(creada.getId());

        Credencial guardada = repository
                .findById(creada.getId())
                .orElseThrow();

        assertEquals(
                EstadoRegistro.ACTIVO,
                guardada.getEstadoRegistro()
        );

        assertNull(guardada.getNombreRegistro());

        assertEquals(CORREO, guardada.getCorreo());
        assertEquals(HASH, guardada.getPasswordHash());

        assertEquals(1L, repository.count());
    }

    // PI-AUTH-05: Rollback ante un fallo transaccional
    @Test
    void revertirCambiosAnteFallo() {

        Credencial creada = crearCredencialPendiente();

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        assertThrows(
                IllegalStateException.class,
                () -> transaction.executeWithoutResult(status -> {

                    Credencial credencial = repository
                            .findById(creada.getId())
                            .orElseThrow();

                    credencial.setEstadoRegistro(
                            EstadoRegistro.ACTIVO
                    );

                    credencial.setNombreRegistro(null);

                    repository.saveAndFlush(credencial);

                    // Simular un fallo antes de confirmar
                    // la transaccion.

                    throw new IllegalStateException(
                            "Fallo transaccional simulado"
                    );
                })
        );

        Credencial guardada = repository
                .findById(creada.getId())
                .orElseThrow();

        assertEquals(
                EstadoRegistro.PENDIENTE,
                guardada.getEstadoRegistro()
        );

        assertEquals(NOMBRE, guardada.getNombreRegistro());
        assertEquals(HASH, guardada.getPasswordHash());

        assertEquals(1L, repository.count());
    }

    // PI-AUTH-06: Independencia de REQUIRES_NEW
    @Test
    void conservarRegistroAunqueFalleTransaccionExterna() {

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        assertThrows(
                IllegalStateException.class,
                () -> transaction.executeWithoutResult(status -> {

                    service.crearPendiente(
                            CORREO,
                            HASH,
                            NOMBRE
                    );

                    // La transaccion externa falla despues
                    // de guardar la credencial pendiente.

                    throw new IllegalStateException(
                            "Fallo de transaccion externa"
                    );
                })
        );

        Credencial guardada = repository
                .findByCorreo(CORREO)
                .orElseThrow();

        assertNotNull(guardada.getId());

        assertEquals(
                EstadoRegistro.PENDIENTE,
                guardada.getEstadoRegistro()
        );

        assertEquals(NOMBRE, guardada.getNombreRegistro());
        assertEquals(HASH, guardada.getPasswordHash());

        assertEquals(1L, repository.count());
    }

    // Metodo auxiliar para ejecutar registros concurrentes

    private int intentarRegistro(
            CountDownLatch preparados,
            CountDownLatch inicio
    ) throws InterruptedException {

        preparados.countDown();

        if (!inicio.await(10, TimeUnit.SECONDS)) {

            throw new IllegalStateException(
                    "Tiempo de espera agotado"
            );
        }

        try {

            service.crearPendiente(
                    CORREO,
                    HASH,
                    NOMBRE
            );

            return 201;

        } catch (ResponseStatusException exception) {

            return exception.getStatusCode().value();
        }
    }

    // PI-AUTH-07: Creacion concurrente de credenciales
    @Test
    void evitarCredencialesDuplicadasConcurrentes()
            throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch preparados =
                new CountDownLatch(2);

        CountDownLatch inicio =
                new CountDownLatch(1);

        try {

            Future<Integer> resultado1 = executor.submit(
                    () -> intentarRegistro(
                            preparados,
                            inicio
                    )
            );

            Future<Integer> resultado2 = executor.submit(
                    () -> intentarRegistro(
                            preparados,
                            inicio
                    )
            );

            assertTrue(
                    preparados.await(10, TimeUnit.SECONDS)
            );

            // Permitir que ambos registros comiencen
            // de manera concurrente.

            inicio.countDown();

            int estado1 = resultado1.get(
                    20,
                    TimeUnit.SECONDS
            );

            int estado2 = resultado2.get(
                    20,
                    TimeUnit.SECONDS
            );

            assertTrue(
                    (estado1 == 201 && estado2 == 409)
                            ||
                            (estado1 == 409 && estado2 == 201)
            );

            assertEquals(1L, repository.count());

            Credencial guardada = repository
                    .findByCorreo(CORREO)
                    .orElseThrow();

            assertEquals(
                    EstadoRegistro.PENDIENTE,
                    guardada.getEstadoRegistro()
            );

            assertEquals(NOMBRE, guardada.getNombreRegistro());
            assertEquals(HASH, guardada.getPasswordHash());

        } finally {

            inicio.countDown();

            executor.shutdownNow();

            executor.awaitTermination(
                    10,
                    TimeUnit.SECONDS
            );
        }
    }
}
