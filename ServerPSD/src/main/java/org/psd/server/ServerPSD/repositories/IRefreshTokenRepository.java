package org.psd.server.ServerPSD.repositories;

import org.psd.server.ServerPSD.model.RefreshToken;
import org.psd.server.ServerPSD.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface IRefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    @Modifying(clearAutomatically=true, flushAutomatically=true)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    int deleteByUser(User user);
}
