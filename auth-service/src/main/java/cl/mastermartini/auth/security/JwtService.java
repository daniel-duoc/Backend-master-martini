
package cl.mastermartini.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

@Service
public class JwtService {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long expiration;
    private final String issuer;

    public JwtService(
            @Value("${jwt.keystore.path}") String path,
            @Value("${jwt.keystore.password}") String password,
            @Value("${jwt.keystore.alias}") String alias,
            @Value("${jwt.expiration}") long expiration,
            @Value("${jwt.issuer}") String issuer) {

        this.expiration = expiration;
        this.issuer = issuer;

        try {
            // Cargar el almacen de claves RSA
            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            try (InputStream input = Files.newInputStream(Path.of(path))) {
                keyStore.load(input, password.toCharArray());
            }

            // Obtener la clave privada
            this.privateKey = (PrivateKey) keyStore.getKey(
                    alias, password.toCharArray());

            // Obtener la clave publica
            this.publicKey = keyStore.getCertificate(alias)
                    .getPublicKey();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error al cargar las claves RSA", e);
        }
    }

    // Generar un token JWT firmado con RSA
    public String generarToken(Long usuarioId, String correo) {

        Date ahora = new Date();
        Date vencimiento = new Date(
                ahora.getTime() + expiration);

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

        return Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Obtener el ID del usuario
    public Long obtenerUsuarioId(String token) {
        return Long.valueOf(
                obtenerClaims(token).getSubject());
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

