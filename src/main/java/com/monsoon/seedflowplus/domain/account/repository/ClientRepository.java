package com.monsoon.seedflowplus.domain.account.repository;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByClientCode(String clientCode);

    boolean existsByClientBrn(String clientBrn);

    boolean existsByClientBrnAndIdNot(String clientBrn, Long id);

}
