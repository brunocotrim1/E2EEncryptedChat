package org.psd.CloudPSD.repositories;

import org.psd.CloudPSD.models.Message;
import org.psd.CloudPSD.models.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IMessageRepository extends JpaRepository<Message, String> {

    @Query("SELECT s FROM Message s WHERE ((s.sender = :user1 AND s.receiver = :user2) OR (s.sender = :user2 AND s.receiver = :user1)) AND s.type = 'PRIVATE'")
    public List<Message> findByUser1AndUser2(@Param("user1")String user1, @Param("user2") String user2);
    @Query("SELECT s FROM Message s WHERE s.receiver = :group AND s.type = 'GROUP'")
    public List<Message> findByGroup(@Param("group")String group);

}
