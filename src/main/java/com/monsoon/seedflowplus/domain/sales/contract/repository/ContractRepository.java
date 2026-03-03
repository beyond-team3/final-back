package com.monsoon.seedflowplus.domain.sales.contract.repository;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface ContractRepository extends JpaRepository<ContractHeader, Long> {

    List<ContractHeader> findAllByStatus(ContractStatus status);

    List<ContractHeader> findByClientOrderByEndDateAsc(Client client);

    // 거래처별 모든 계약 종료일 조회
    @Query("SELECT c.client.id, c.endDate FROM ContractHeader c ORDER BY c.endDate ASC")
    List<Object[]> findAllClientEndDatesRaw();

    default Map<Long, List<LocalDate>> findAllClientEndDates() {
        return findAllClientEndDatesRaw().stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (LocalDate) row[1], Collectors.toList())
                ));
    }
}
