package com.monsoon.seedflowplus.domain.account.repository;

import com.monsoon.seedflowplus.domain.account.entity.ClientCrop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientCropRepository extends JpaRepository<ClientCrop, Long> {
    List<ClientCrop> findAllByClientId(Long clientId);
}
