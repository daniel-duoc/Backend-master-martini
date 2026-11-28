package cl.mastermartini.usuarios.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;
import java.security.cert.CertificateFactory;

@Service
public class JwtService {

    private final JwtParser jwtParser;

    public JwtService(
            @Value("${jwt.public-key.path}") String publicKeyPath,
            @Value("${jwt.issuer}") String issuer
    ) {

        // Validar los parametros de configuracion
        if (publicKeyPath == null || publicKeyPath.isBlank()) {

            throw new IllegalArgumentException(
                    "La ruta de la clave publica es obligatoria"
            );
        }

        if (issuer == null || issuer.isBlank()) {

            throw new IllegalArgumentException(
                    "El emisor del token es obligatorio"
            );
        }

        try (InputStream input = Files.newInputStream(
                Path.of(publicKeyPath))) {

            // Cargar el certificado X.509
            CertificateFactory factory =
                    CertificateFactory.getInstance("X.509");

            // Obtener la clave publica RSA
            RSAPublicKey publicKey = (RSAPublicKey) factory
                    .generateCertificate(input)
                    .getPublicKey();

            // Comprobar el tamano minimo de la clave
            if (publicKey.getModulus().bitLength() < 2048) {

                throw new IllegalStateException(
                        "La clave RSA debe tener al menos 2048 bits"
                );
            }

            // Configurar el analizador JWT una sola vez
            this.jwtParser = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(issuer)
                    .build();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "No se pudo cargar la clave publica RSA",
                    e
            );
        }
    }

    // Validar la firma y obtener los datos del token
    private Claims obtenerClaims(String token) {

        var jwt = jwtParser.parseSignedClaims(token);

        // Aceptar exclusivamente el algoritmo RS256
        if (!"RS256".equals(jwt.getHeader().getAlgorithm())) {

            throw new IllegalArgumentException(
                    "Algoritmo de firma no permitido"
            );
        }

        Claims claims = jwt.getPayload();

        // Exigir una fecha de expiracion
        if (claims.getExpiration() == null) {

            throw new IllegalArgumentException(
                    "El token no contiene fecha de expiracion"
            );
        }

        // Validar el identificador del usuario
        validarIdentificador(claims.getSubject());

        // Validar la existencia del correo
        String correo = claims.get(
                "correo",
                String.class
        );

        if (correo == null || correo.isBlank()) {

            throw new IllegalArgumentException(
                    "El token no contiene un correo valido"
            );
        }

        return claims;
    }

    // Validar y convertir el identificador
    private Long validarIdentificador(String subject) {

        if (subject == null || subject.isBlank()) {

            throw new IllegalArgumentException(
                    "El token no contiene un identificador"
            );
        }

        Long usuarioId = Long.valueOf(subject);

        if (usuarioId <= 0) {

            throw new IllegalArgumentException(
                    "Identificador de usuario invalido"
            );
        }

        return usuarioId;
    }

    // Validar el token y obtener el identificador
    public Long obtenerUsuarioId(String token) {

        return validarIdentificador(
                obtenerClaims(token).getSubject()
        );
    }

    // Obtener el correo del usuario
    public String obtenerCorreo(String token) {

        return obtenerClaims(token)
                .get("correo", String.class);
    }

    // Comprobar si el token es valido
    public boolean validarToken(String token) {

        try {

            obtenerClaims(token);
            return true;

        } catch (Exception e) {

            return false;
        }
    }
}