package org.psd.CloudPSD.repositories;

import org.psd.CloudPSD.models.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IMessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT s FROM Message s WHERE (s.sender = :user1 AND s.receiver = :user2) OR (s.sender = :user2 AND s.receiver = :user1)")
    public List<Message> findByUser1AndUser2(@Param("user1")String user1, @Param("user2") String user2);
}
