package cl.mastermartini.auth.repository;

import cl.mastermartini.auth.model.Credencial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredencialRepository
        extends JpaRepository<Credencial, Long> {

    Optional<Credencial> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}