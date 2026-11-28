package cl.mastermartini.usuarios.service;

import cl.mastermartini.usuarios.model.PerfilUsuario;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.hibernate.exception.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PerfilTransaccionalService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PerfilUsuario insertarPerfil(
            Long id,
            String nombre,
            String correo
    ) {

        PerfilUsuario perfil = new PerfilUsuario(
                id,
                nombre,
                correo
        );

        try {

            entityManager.persist(perfil);

            entityManager.flush();

            return perfil;

        } catch (ConstraintViolationException exception) {

            throw new DataIntegrityViolationException(
                    "Conflicto de integridad al insertar el perfil",
                    exception
            );
        }
    }
}