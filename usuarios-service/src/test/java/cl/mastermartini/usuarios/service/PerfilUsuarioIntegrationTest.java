package cl.mastermartini.usuarios.service;

import cl.mastermartini.usuarios.dto.ActualizarPerfilRequest;
import cl.mastermartini.usuarios.dto.CrearPerfilRequest;
import cl.mastermartini.usuarios.dto.PerfilResponse;
import cl.mastermartini.usuarios.model.PerfilUsuario;
import cl.mastermartini.usuarios.repository.PerfilUsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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
@Import({
        PerfilUsuarioService.class,
        PerfilTransaccionalService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PerfilUsuarioIntegrationTest {

    @Autowired
    private PerfilUsuarioRepository repository;

    @Autowired
    private PerfilUsuarioService service;

    @Autowired
    private PerfilTransaccionalService perfilTransaccional;

    private static final Long ID = 1L;
    private static final String NOMBRE = "Usuario Prueba";
    private static final String CORREO = "prueba@example.com";

    @BeforeEach
    void limpiarBaseDeDatos() {

        repository.deleteAll();
    }

    // Metodo auxiliar para crear un perfil

    private PerfilResponse crearPerfilOriginal() {

        return service.crearPerfil(
                new CrearPerfilRequest(
                        ID,
                        NOMBRE,
                        CORREO
                )
        );
    }

    // Metodo auxiliar para actualizar un perfil

    private ActualizarPerfilRequest crearActualizacion() {

        return new ActualizarPerfilRequest(
                "Usuario Actualizado",
                "912345678",
                "Pasteleria",
                "Chocolateria",
                "https://example.com/foto.png",
                "Pasteleria Prueba",
                "Santiago",
                "Administrador",
                "Empresa de prueba"
        );
    }

    // Metodo auxiliar para comprobar los datos actualizados

    private void comprobarPerfilActualizado(
            PerfilUsuario perfil
    ) {

        assertEquals(ID, perfil.getId());
        assertEquals(CORREO, perfil.getCorreo());

        assertEquals(
                "Usuario Actualizado",
                perfil.getNombre()
        );

        assertEquals(
                "912345678",
                perfil.getTelefono()
        );

        assertEquals(
                "Pasteleria",
                perfil.getTipoNegocio()
        );

        assertEquals(
                "Chocolateria",
                perfil.getIntereses()
        );

        assertEquals(
                "https://example.com/foto.png",
                perfil.getFoto()
        );

        assertEquals(
                "Pasteleria Prueba",
                perfil.getNombreEmpresa()
        );

        assertEquals(
                "Santiago",
                perfil.getDireccionEmpresa()
        );

        assertEquals(
                "Administrador",
                perfil.getCargoEmpresa()
        );

        assertEquals(
                "Empresa de prueba",
                perfil.getDescripcionEmpresa()
        );
    }

    // PI-USU-01: Persistencia real del perfil
    @Test
    void persistirPerfilCorrectamente() {

        PerfilResponse respuesta = crearPerfilOriginal();

        assertEquals(ID, respuesta.id());
        assertEquals(NOMBRE, respuesta.nombre());
        assertEquals(CORREO, respuesta.correo());

        PerfilUsuario guardado = repository
                .findById(ID)
                .orElseThrow();

        assertEquals(ID, guardado.getId());
        assertEquals(NOMBRE, guardado.getNombre());
        assertEquals(CORREO, guardado.getCorreo());

        assertEquals(1L, repository.count());
    }

    // PI-USU-02: Creacion idempotente
    @Test
    void crearPerfilVariasVecesSinDuplicados() {

        PerfilResponse primeraRespuesta =
                crearPerfilOriginal();

        PerfilResponse segundaRespuesta =
                crearPerfilOriginal();

        PerfilResponse terceraRespuesta =
                crearPerfilOriginal();

        assertEquals(
                primeraRespuesta,
                segundaRespuesta
        );

        assertEquals(
                primeraRespuesta,
                terceraRespuesta
        );

        assertEquals(1L, repository.count());

        PerfilUsuario guardado = repository
                .findById(ID)
                .orElseThrow();

        assertEquals(NOMBRE, guardado.getNombre());
        assertEquals(CORREO, guardado.getCorreo());
    }

    // PI-USU-03: Mismo ID con correo diferente
    @Test
    void rechazarMismoIdConCorreoDiferente() {

        crearPerfilOriginal();

        CrearPerfilRequest request =
                new CrearPerfilRequest(
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

        // Comprobar tambien la restriccion real del ID

        assertThrows(
                DataIntegrityViolationException.class,
                () -> perfilTransaccional.insertarPerfil(
                        ID,
                        "Otro Usuario",
                        "otro@example.com"
                )
        );

        assertEquals(1L, repository.count());

        PerfilUsuario guardado = repository
                .findById(ID)
                .orElseThrow();

        assertEquals(NOMBRE, guardado.getNombre());
        assertEquals(CORREO, guardado.getCorreo());
    }

    // PI-USU-04: Mismo correo con ID diferente
    @Test
    void rechazarMismoCorreoConIdDiferente() {

        crearPerfilOriginal();

        CrearPerfilRequest request =
                new CrearPerfilRequest(
                        2L,
                        "Otro Usuario",
                        CORREO
                );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.crearPerfil(request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        // Comprobar la restriccion unica de la base de datos

        assertThrows(
                DataIntegrityViolationException.class,
                () -> perfilTransaccional.insertarPerfil(
                        2L,
                        "Otro Usuario",
                        CORREO
                )
        );

        assertEquals(1L, repository.count());

        assertFalse(repository.existsById(2L));

        PerfilUsuario guardado = repository
                .findById(ID)
                .orElseThrow();

        assertEquals(NOMBRE, guardado.getNombre());
        assertEquals(CORREO, guardado.getCorreo());
    }

    // PI-USU-05: Actualizacion real del perfil
    @Test
    void persistirModificacionesDelPerfil() {

        crearPerfilOriginal();

        PerfilResponse respuesta =
                service.actualizarPerfil(
                        ID,
                        crearActualizacion()
                );

        assertEquals(
                "Usuario Actualizado",
                respuesta.nombre()
        );

        assertEquals(CORREO, respuesta.correo());
        assertEquals(ID, respuesta.id());

        PerfilUsuario guardado = repository
                .findById(ID)
                .orElseThrow();

        comprobarPerfilActualizado(guardado);

        assertEquals(1L, repository.count());
    }

    // PI-USU-06: Conservar datos durante un reintento
    @Test
    void conservarPerfilActualizadoDuranteReintento() {

        crearPerfilOriginal();

        service.actualizarPerfil(
                ID,
                crearActualizacion()
        );

        // Simular que auth-service vuelve a solicitar
        // la creacion del perfil original.

        PerfilResponse respuesta =
                service.crearPerfil(
                        new CrearPerfilRequest(
                                ID,
                                NOMBRE,
                                CORREO
                        )
                );

        assertEquals(
                "Usuario Actualizado",
                respuesta.nombre()
        );

        PerfilUsuario guardado = repository
                .findById(ID)
                .orElseThrow();

        comprobarPerfilActualizado(guardado);

        assertEquals(1L, repository.count());
    }

    // Metodo auxiliar para la prueba concurrente

    private PerfilResponse intentarCrearPerfil(
            CountDownLatch preparados,
            CountDownLatch inicio
    ) throws InterruptedException {

        preparados.countDown();

        if (!inicio.await(10, TimeUnit.SECONDS)) {

            throw new IllegalStateException(
                    "Tiempo de espera agotado"
            );
        }

        return crearPerfilOriginal();
    }

    // PI-USU-07: Creacion concurrente compatible
    @Test
    void evitarPerfilesDuplicadosConcurrentes()
            throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch preparados =
                new CountDownLatch(2);

        CountDownLatch inicio =
                new CountDownLatch(1);

        try {

            Future<PerfilResponse> resultado1 =
                    executor.submit(
                            () -> intentarCrearPerfil(
                                    preparados,
                                    inicio
                            )
                    );

            Future<PerfilResponse> resultado2 =
                    executor.submit(
                            () -> intentarCrearPerfil(
                                    preparados,
                                    inicio
                            )
                    );

            assertTrue(
                    preparados.await(
                            10,
                            TimeUnit.SECONDS
                    )
            );

            inicio.countDown();

            PerfilResponse respuesta1 =
                    resultado1.get(
                            20,
                            TimeUnit.SECONDS
                    );

            PerfilResponse respuesta2 =
                    resultado2.get(
                            20,
                            TimeUnit.SECONDS
                    );

            assertEquals(ID, respuesta1.id());
            assertEquals(ID, respuesta2.id());

            assertEquals(NOMBRE, respuesta1.nombre());
            assertEquals(NOMBRE, respuesta2.nombre());

            assertEquals(CORREO, respuesta1.correo());
            assertEquals(CORREO, respuesta2.correo());

            assertEquals(
                    respuesta1,
                    respuesta2
            );

            assertEquals(1L, repository.count());

            PerfilUsuario guardado = repository
                    .findById(ID)
                    .orElseThrow();

            assertEquals(NOMBRE, guardado.getNombre());
            assertEquals(CORREO, guardado.getCorreo());

        } finally {

            inicio.countDown();

            executor.shutdownNow();

            executor.awaitTermination(
                    10,
                    TimeUnit.SECONDS
            );
        }
    }

    // PI-USU-08: Recuperacion tras conflicto transaccional
    @Test
    void recuperarOperacionDespuesDeConflicto() {

        crearPerfilOriginal();

        // Forzar un conflicto real en la base de datos.

        assertThrows(
                DataIntegrityViolationException.class,
                () -> perfilTransaccional.insertarPerfil(
                        2L,
                        "Usuario Duplicado",
                        CORREO
                )
        );

        // Comprobar que la transaccion fallida
        // no dejo registros incorrectos.

        assertEquals(1L, repository.count());

        assertFalse(repository.existsById(2L));

        // Crear posteriormente un perfil valido.

        PerfilResponse nuevoPerfil =
                service.crearPerfil(
                        new CrearPerfilRequest(
                                2L,
                                "Segundo Usuario",
                                "segundo@example.com"
                        )
                );

        assertEquals(2L, nuevoPerfil.id());

        assertEquals(
                "Segundo Usuario",
                nuevoPerfil.nombre()
        );

        assertEquals(
                "segundo@example.com",
                nuevoPerfil.correo()
        );

        assertEquals(2L, repository.count());

        PerfilUsuario original = repository
                .findById(ID)
                .orElseThrow();

        PerfilUsuario segundo = repository
                .findById(2L)
                .orElseThrow();

        assertEquals(NOMBRE, original.getNombre());
        assertEquals(CORREO, original.getCorreo());

        assertEquals(
                "Segundo Usuario",
                segundo.getNombre()
        );

        assertEquals(
                "segundo@example.com",
                segundo.getCorreo()
        );
    }
}