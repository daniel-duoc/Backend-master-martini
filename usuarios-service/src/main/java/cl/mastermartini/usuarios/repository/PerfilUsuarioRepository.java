package cl.mastermartini.usuarios.repository;

import cl.mastermartini.usuarios.model.PerfilUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerfilUsuarioRepository
        extends JpaRepository<PerfilUsuario, Long> {

    Optional<PerfilUsuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}
