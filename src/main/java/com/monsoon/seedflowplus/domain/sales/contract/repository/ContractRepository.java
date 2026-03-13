package com.monsoon.seedflowplus.domain.sales.contract.repository;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ContractRepository extends JpaRepository<ContractHeader, Long> {

        @Modifying(clearAutomatically = true)
        @Query("UPDATE ContractHeader c SET c.status = :newStatus " +
                        "WHERE c.status = :oldStatus AND c.startDate <= :today")
        int updateStatusForActivation(@Param("oldStatus") ContractStatus oldStatus,
                        @Param("newStatus") ContractStatus newStatus,
                        @Param("today") LocalDate today);

        @Modifying(clearAutomatically = true)
        @Query("UPDATE ContractHeader c SET c.status = :newStatus " +
                        "WHERE c.status = :oldStatus AND c.endDate < :today")
        int updateStatusForExpiration(@Param("oldStatus") ContractStatus oldStatus,
                        @Param("newStatus") ContractStatus newStatus,
                        @Param("today") LocalDate today);

        List<ContractHeader> findAllByStatus(ContractStatus status);

        List<ContractHeader> findByClientOrderByEndDateAsc(Client client);

        List<ContractHeader> findByClientAndStatusOrderByEndDateAsc(Client client, ContractStatus status);

        @Query("""
                        SELECT c
                        FROM ContractHeader c
                        JOIN FETCH c.deal d
                        LEFT JOIN FETCH d.ownerEmp
                        JOIN FETCH c.client
                        WHERE c.id = :contractId
                        """)
        Optional<ContractHeader> findByIdWithScheduleRelations(@Param("contractId") Long contractId);

        @Query("SELECT c FROM ContractHeader c WHERE (c.author.id = :employeeId OR c.client.managerEmployee.id = :employeeId) AND c.status <> :status ORDER BY c.createdAt DESC")
        List<ContractHeader> findByAuthorIdOrClientManagerEmployeeIdAndStatusNot(@Param("employeeId") Long employeeId,
                        @Param("status") ContractStatus status);

        /**
         * 거래처별 활성 계약 조회 (실제 ACTIVE_CONTRACT + 승인 즉시 활성 간주되는 COMPLETED 포함)
         */
        @Query("SELECT c FROM ContractHeader c " +
                        "WHERE c.client = :client " +
                        "AND (c.status = :activeStatus " +
                        "OR (c.status = :completedStatus AND c.startDate <= :today AND c.endDate >= :today)) " +
                        "ORDER BY c.endDate ASC")
        List<ContractHeader> findActiveContractsByClient(@Param("client") Client client,
                        @Param("today") LocalDate today,
                        @Param("activeStatus") ContractStatus activeStatus,
                        @Param("completedStatus") ContractStatus completedStatus);

        /**
         * 반려된 계약서 중 재작성이 필요한 '활성' 건만 조회합니다.
         * 1. 해당 Deal에 진행 중(승인 대기 등)인 다른 계약서가 없어야 함.
         * 2. 동일 Deal 내 반려 건 중 가장 최신(ID 기준) 건만 노출.
         */
        @Query("SELECT c FROM ContractHeader c " +
                        "WHERE (c.author.id = :employeeId OR c.client.managerEmployee.id = :employeeId) " +
                        "AND c.status IN :statuses " +
                        "AND c.id = (SELECT MAX(c2.id) FROM ContractHeader c2 " +
                        "            WHERE c2.deal.id = c.deal.id " +
                        "            AND c2.status <> :deletedStatus) " +
                        "AND NOT EXISTS (SELECT 1 FROM ContractHeader c3 " +
                        "                WHERE c3.deal.id = c.deal.id " +
                        "                AND c3.id > c.id " +
                        "                AND c3.status NOT IN :statuses " +
                        "                AND c3.status <> :deletedStatus)")
        List<ContractHeader> findActiveRejectedContracts(
                        @Param("employeeId") Long employeeId,
                        @Param("statuses") List<ContractStatus> statuses,
                        @Param("deletedStatus") ContractStatus deletedStatus);

        // 계약 코드와 거래처 ID로 계약 존재 여부 확인
        boolean existsByContractCodeAndClientId(String contractCode, Long clientId);

        // 상태와 시작일을 기준으로 계약 조회
        List<ContractHeader> findByStatusAndStartDateLessThanEqual(ContractStatus status, LocalDate date);

        // 상태와 종료일을 기준으로 계약 조회
        List<ContractHeader> findByStatusAndEndDateLessThan(ContractStatus status, LocalDate date);

        // 거래처별 모든 계약 종료일 조회
        @Query("SELECT c.client.id, c.endDate FROM ContractHeader c ORDER BY c.endDate ASC")
        List<Object[]> findAllClientEndDatesRaw();

        default Map<Long, List<LocalDate>> findAllClientEndDates() {
                return findAllClientEndDatesRaw().stream()
                                .collect(Collectors.groupingBy(
                                                row -> (Long) row[0],
                                                Collectors.mapping(row -> (LocalDate) row[1], Collectors.toList())));
        }
}
