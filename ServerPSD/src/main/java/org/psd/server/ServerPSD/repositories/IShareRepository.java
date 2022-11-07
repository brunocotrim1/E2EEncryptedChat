package org.psd.server.ServerPSD.repositories;

import org.psd.server.ServerPSD.model.Share;
import org.psd.server.ServerPSD.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IShareRepository extends JpaRepository<Share, Long> {
    @Query("SELECT s FROM Share s WHERE (s.user1 = :user1 AND s.user2 = :user2) OR (s.user1 = :user2 AND s.user2 = :user1)")
    public Share findByUser1AndUser2(@Param("user1")String user1,@Param("user2") String user2);
}
