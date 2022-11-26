package org.psd.server.ServerPSD.repositories;

import org.psd.server.ServerPSD.model.GroupChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IGroupChatRepository extends JpaRepository<GroupChat, Integer> {
    public Optional<GroupChat> findByName(String topic);
}
