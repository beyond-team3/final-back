package com.monsoon.seedflowplus.domain.account.repository;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    Optional<User> findByEmployeeId(Long employeeId);

    Optional<User> findByClientId(Long clientId);

    List<User> findAllByEmployeeIdIn(List<Long> employeeIds);

    List<User> findAllByClientIdIn(List<Long> clientIds);

    List<User> findAllByRole(Role role);

    List<User> findAllByEmployeeIsNotNull();

}
