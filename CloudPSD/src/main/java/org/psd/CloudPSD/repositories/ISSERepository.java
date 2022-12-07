package org.psd.CloudPSD.repositories;

import org.psd.CloudPSD.models.SSETable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ISSERepository extends JpaRepository<SSETable, Long> {
    public List<SSETable> findByUsername(String username);
    public void deleteAllByUsername(String username);
}
