package org.psd.server.ServerPSD.security.jwt;

import org.psd.server.ServerPSD.exceptions.TokenRefreshException;
import org.psd.server.ServerPSD.model.RefreshToken;
import org.psd.server.ServerPSD.repositories.IRefreshTokenRepository;
import org.psd.server.ServerPSD.repositories.IUsersRepository;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class RefreshTokenService {


    private Long refreshTokenDurationMs = 1000 * 60 * 60 * 24 * 7L;

    @Autowired
    private IRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private IUsersRepository userRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken createRefreshToken(String username) {
        RefreshToken token = new RefreshToken();

        token.setUser(userRepository.findById(username).get());
        token.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        token.setToken(UUID.randomUUID().toString());
        refreshTokenRepository.save(token);
        return token;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public int deleteByUserId(String username) {
        return refreshTokenRepository.deleteByUser(userRepository.findById(username).get());
    }
}
