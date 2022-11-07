package org.psd.server.ServerPSD.repositories;

import org.psd.server.ServerPSD.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IUsersRepository extends JpaRepository<User, String> {
    public Optional<User> findById(String username);
}
