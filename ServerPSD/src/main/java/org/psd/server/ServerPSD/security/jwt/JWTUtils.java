package org.psd.server.ServerPSD.security.jwt;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
@Component
public class JWTUtils {
    private static final Logger logger = LoggerFactory.getLogger(JWTUtils.class);

    @Value("${jwt.refresh-secret}")
    private String refreshTokenSecret;
    @Value("${jwt.access-secret}")
    private String accessSecret;

    private int expirationTime = 1000 * 60 * 60 * 24 * 7;

    private int accessExpirationTime = 1000 * 60 * 15;


    public String getUserNameFromJwtAccessToken(String token) {
        return Jwts.parser().setSigningKey(accessSecret).parseClaimsJws(token).getBody().getSubject();
    }

    public String getUsernameFromJwtRefreshToken(String token) {
        return Jwts.parser().setSigningKey(refreshTokenSecret).parseClaimsJws(token).getBody().getSubject();
    }

    public String generateAccessToken(String username){
        return Jwts.builder().setSubject(username).setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + accessExpirationTime)).signWith(SignatureAlgorithm.HS512, accessSecret)
                .compact();
    }

    public String generateRefreshToken(String username){
        return Jwts.builder().setSubject(username).setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + expirationTime)).signWith(SignatureAlgorithm.HS512, refreshTokenSecret)
                .compact();
    }



    public boolean validateJwtAccessToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(accessSecret).parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }
    public boolean validateJwtRefreshToken(String refreshToken) {
        try {
            Jwts.parser().setSigningKey(refreshTokenSecret).parseClaimsJws(refreshToken);
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }

}