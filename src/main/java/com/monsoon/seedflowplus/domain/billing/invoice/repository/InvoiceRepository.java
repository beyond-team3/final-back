package com.monsoon.seedflowplus.domain.billing.invoice.repository;

import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByClientId(Long clientId);

    List<Invoice> findAllByContractId(Long contractId);

    List<Invoice> findAllByStatus(InvoiceStatus status);

    // 특정 계약의 DRAFT/PUBLISHED 상태 청구서 존재 여부 (중복 생성 방지)
    boolean existsByContractIdAndStatusIn(Long contractId, List<InvoiceStatus> statuses);

    // 코드 채번용
    boolean existsByInvoiceCode(String invoiceCode);

    List<Invoice> findAllByEmployeeId(Long employeeId);

    @Query("SELECT MAX(CAST(SUBSTRING(i.invoiceCode, LENGTH(:prefix) + 1) AS integer)) " +
            "FROM Invoice i WHERE i.invoiceCode LIKE CONCAT(:prefix, '%')")
    Optional<Integer> findMaxSuffixByPrefix(@Param("prefix") String prefix);
}