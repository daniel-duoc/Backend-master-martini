package cl.mastermartini.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

@Service
public class JwtService {

    private final RSAPrivateKey privateKey;
    private final JwtParser jwtParser;
    private final long expiration;
    private final String issuer;

    public JwtService(
            @Value("${jwt.keystore.path}") String path,
            @Value("${jwt.keystore.password}") String password,
            @Value("${jwt.keystore.alias}") String alias,
            @Value("${jwt.expiration}") long expiration,
            @Value("${jwt.issuer}") String issuer
    ) {

        // Validar los parametros de configuracion
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    "La ruta del keystore es obligatoria"
            );
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "La contrasena del keystore es obligatoria"
            );
        }

        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException(
                    "El alias del keystore es obligatorio"
            );
        }

        if (expiration <= 0) {
            throw new IllegalArgumentException(
                    "La expiracion debe ser mayor que cero"
            );
        }

        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException(
                    "El emisor del token es obligatorio"
            );
        }

        this.expiration = expiration;
        this.issuer = issuer;

        try {

            KeyStore keyStore =
                    KeyStore.getInstance("PKCS12");

            try (InputStream input =
                         Files.newInputStream(Path.of(path))) {

                keyStore.load(
                        input,
                        password.toCharArray()
                );
            }

            // Cargar la clave privada RSA
            this.privateKey = (RSAPrivateKey)
                    keyStore.getKey(
                            alias,
                            password.toCharArray()
                    );

            // Cargar la clave publica RSA
            RSAPublicKey publicKey = (RSAPublicKey)
                    keyStore.getCertificate(alias)
                            .getPublicKey();

            // Comprobar la existencia de las claves
            if (privateKey == null || publicKey == null) {
                throw new IllegalStateException(
                        "No se encontraron las claves RSA"
                );
            }

            // Comprobar el tamano minimo de las claves
            if (privateKey.getModulus().bitLength() < 2048
                    || publicKey.getModulus().bitLength() < 2048) {

                throw new IllegalStateException(
                        "Las claves RSA deben tener al menos 2048 bits"
                );
            }

            // Comprobar que las claves correspondan
            if (!privateKey.getModulus()
                    .equals(publicKey.getModulus())) {

                throw new IllegalStateException(
                        "Las claves RSA no corresponden"
                );
            }

            // Configurar el analizador JWT una sola vez
            this.jwtParser = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(issuer)
                    .build();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Error al cargar las claves RSA",
                    e
            );
        }
    }

    // Generar un token JWT firmado con RS256
    public String generarToken(
            Long usuarioId,
            String correo
    ) {

        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "Identificador de usuario invalido"
            );
        }

        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException(
                    "El correo del usuario es obligatorio"
            );
        }

        Date ahora = new Date();

        Date vencimiento = new Date(
                Math.addExact(
                        ahora.getTime(),
                        expiration
                )
        );

        return Jwts.builder()
                .subject(usuarioId.toString())
                .claim("correo", correo)
                .issuer(issuer)
                .issuedAt(ahora)
                .expiration(vencimiento)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    // Obtener los datos de un token validado
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

    // Obtener el ID del usuario
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