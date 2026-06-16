package com.smarterp.security.jwt;

import com.smarterp.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Valida el token JWT emitido por la API de Autenticación
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            log.error("Firma JWT inválida");
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException ex) {
            log.error("Token JWT malformado");
            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.error("Token JWT expirado");
            return false;
        } catch (io.jsonwebtoken.UnsupportedJwtException ex) {
            log.error("Token JWT no soportado");
            return false;
        } catch (IllegalArgumentException ex) {
            log.error("Token JWT vacío o nulo");
            return false;
        }
    }

    /**
     * Extrae el email del usuario desde el token
     */
    public String getEmailFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * Extrae el user_id del token (custom claim)
     */
    public String getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("userId", String.class);
    }

    /**
     * Extrae el business_id del token (custom claim)
     */
    public String getBusinessIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("businessId", String.class);
    }

    /**
     * Extrae el role del token (custom claim)
     */
    public String getRoleFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    /**
     * Extrae todos los claims del token
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Obtiene la fecha de expiración del token
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getExpiration();
    }
}