package com.monsoon.seedflowplus.domain.billing.payment.repository;

import com.monsoon.seedflowplus.domain.billing.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByInvoice_Id(Long invoiceId);

    List<Payment> findAllByClient_Id(Long clientId);

    Optional<Payment> findByIdAndClientId(Long id, Long clientId);

    Optional<Payment> findByInvoiceId(Long invoiceId);

    // 코드 채번용
    @Query("SELECT MAX(CAST(SUBSTRING(p.paymentCode, LENGTH(:prefix) + 1) AS integer)) " +
            "FROM Payment p WHERE p.paymentCode LIKE CONCAT(:prefix, '%')")
    Optional<Integer> findMaxSuffixByPrefix(@Param("prefix") String prefix);
}