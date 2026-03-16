package com.monsoon.seedflowplus.domain.account.repository;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT c FROM Client c WHERE c.id = :id")
    Optional<Client> findByIdWithLock(@Param("id") Long id);

    Optional<Client> findByClientCode(String clientCode);

    boolean existsByClientBrn(String clientBrn);

    boolean existsByClientBrnAndIdNot(String clientBrn, Long id);

    List<Client> findAllByManagerEmployeeId(Long employeeId);

    boolean existsByIdAndManagerEmployeeId(Long id, Long managerEmployeeId);

    @Query("SELECT c FROM Client c WHERE NOT EXISTS (SELECT 1 FROM User u WHERE u.client = c)")
    List<Client> findAllUnregistered();

    @Query("SELECT DISTINCT c FROM Client c LEFT JOIN FETCH c.crops WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL AND c.managerEmployee.id = :employeeId")
    List<Client> findAllByManagerEmployeeIdWithCropsAndCoordinates(@Param("employeeId") Long employeeId);

    @Query("SELECT DISTINCT c FROM Client c LEFT JOIN FETCH c.crops WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL")
    List<Client> findAllWithCropsAndCoordinates();
}
