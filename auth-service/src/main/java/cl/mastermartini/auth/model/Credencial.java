package cl.mastermartini.auth.model;

import jakarta.persistence.*;

@Entity
@Table(name = "credenciales")
public class Credencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String correo;

    @Column(nullable = false)
    private String passwordHash;

    // Nombre necesario para recuperar un registro pendiente
    @Column(length = 255)
    private String nombreRegistro;

    // Estado actual del registro
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRegistro estadoRegistro =
            EstadoRegistro.PENDIENTE;

    public Credencial() {
    }

    // Constructor anterior
    public Credencial(String correo, String passwordHash) {
        this.correo = correo;
        this.passwordHash = passwordHash;
    }

    // Constructor para el nuevo proceso de registro
    public Credencial(
            String correo,
            String passwordHash,
            String nombreRegistro
    ) {
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.nombreRegistro = nombreRegistro;
        this.estadoRegistro = EstadoRegistro.PENDIENTE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNombreRegistro() {
        return nombreRegistro;
    }

    public void setNombreRegistro(String nombreRegistro) {
        this.nombreRegistro = nombreRegistro;
    }

    public EstadoRegistro getEstadoRegistro() {
        return estadoRegistro;
    }

    public void setEstadoRegistro(EstadoRegistro estadoRegistro) {
        this.estadoRegistro = estadoRegistro;
    }
}