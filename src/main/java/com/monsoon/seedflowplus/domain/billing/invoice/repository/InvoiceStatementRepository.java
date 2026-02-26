package com.monsoon.seedflowplus.domain.billing.invoice.repository;

import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceStatementRepository extends JpaRepository<InvoiceStatement, Long> {

    List<InvoiceStatement> findAllByInvoiceId(Long invoiceId);

    // 특정 명세서가 포함된(included=true) 청구서 연결 조회
    List<InvoiceStatement> findAllByStatementIdAndIncludedTrue(Long statementId);

    // 청구 가능한 명세서 조회:
    // 1) InvoiceStatement에 없는 명세서
    // 2) InvoiceStatement에 있지만 included=false인 명세서 (영업사원이 제외한 것)
    // → 특정 계약(contractId)에 속한 주문의 명세서 중 위 조건을 만족하는 것
    @Query("""
            SELECT s FROM Statement s
            JOIN s.orderHeader o
            WHERE o.contract.id = :contractId
            AND s.status = 'ISSUED'
            AND (
                NOT EXISTS (
                    SELECT 1 FROM InvoiceStatement ist
                    WHERE ist.statement = s AND ist.included = true
                )
            )
            """)
    List<com.monsoon.seedflowplus.domain.billing.statement.entity.Statement>
    findBillableStatements(@Param("contractId") Long contractId);
}