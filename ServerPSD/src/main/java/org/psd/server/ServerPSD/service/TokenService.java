package org.psd.server.ServerPSD.service;

import org.psd.server.ServerPSD.model.RefreshToken;
import org.psd.server.ServerPSD.model.User;
import org.psd.server.ServerPSD.repositories.IRefreshTokenRepository;
import org.psd.server.ServerPSD.security.jwt.JWTUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class TokenService {
    @Autowired
    IRefreshTokenRepository refreshTokenRepository;
    @Autowired
    JWTUtils jwtUtils;
    @Autowired
    UserService userService;
    private int expirationTime = 1000 * 60 * 60 * 24 * 7;
    public RefreshToken creteRefreshToken(String username){
        RefreshToken refreshToken = new RefreshToken();
        String token = jwtUtils.generateRefreshToken(username);
        User user = userService.getUser(username);
        Instant expiresAt = Instant.now().plusMillis(expirationTime);
        refreshToken.setToken(Sha512DigestUtils.shaHex(token));
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(expiresAt);
        refreshTokenRepository.save(refreshToken);
        return new RefreshToken(token,user,expiresAt);
    }

    public String createAccessToken(String username){
        return jwtUtils.generateAccessToken(username);
    }

    public void deleteRefreshToken(String username){
        refreshTokenRepository.deleteByUser(userService.getUser(username));
    }

    public String getUsernameFromRefreshToken(String token){
        return jwtUtils.getUsernameFromJwtRefreshToken(token);
    }

    public RefreshToken getRefreshToken(String token){
       return refreshTokenRepository.findByToken(token).orElse(null);
    }

    public boolean validateRefreshToken(String token){
        return jwtUtils.validateJwtRefreshToken(token);
    }

    public String verifyToken(String token) {
        return jwtUtils.validateJwtAccessToken(token) ?  jwtUtils.getUserNameFromJwtAccessToken(token) : "";
    }
}
