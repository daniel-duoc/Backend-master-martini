package cl.mastermartini.usuarios.dto;

public record PerfilResponse(

        Long id,
        String nombre,
        String correo,
        String telefono,
        String tipoNegocio,
        String intereses,
        String foto,
        String nombreEmpresa,
        String direccionEmpresa,
        String cargoEmpresa,
        String descripcionEmpresa

) {
}
