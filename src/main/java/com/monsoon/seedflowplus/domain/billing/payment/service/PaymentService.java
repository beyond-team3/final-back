package com.monsoon.seedflowplus.domain.billing.payment.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import com.monsoon.seedflowplus.domain.billing.invoice.repository.InvoiceRepository;
import com.monsoon.seedflowplus.domain.billing.payment.dto.request.PaymentCreateRequest;
import com.monsoon.seedflowplus.domain.billing.payment.dto.response.PaymentListResponse;
import com.monsoon.seedflowplus.domain.billing.payment.dto.response.PaymentResponse;
import com.monsoon.seedflowplus.domain.billing.payment.entity.Payment;
import com.monsoon.seedflowplus.domain.billing.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;

    /**
     * 결제 처리
     * 1. 청구서 조회 및 상태 검증 (PUBLISHED만 가능)
     * 2. 중복 결제 방지
     * 3. Payment 저장
     * 4. Invoice 상태 PAID로 변경
     */
    @Transactional
    public PaymentResponse processPayment(PaymentCreateRequest request, Long clientId) {
        // 1. 청구서 조회
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new CoreException(ErrorType.INVOICE_NOT_FOUND));

        // 2. PUBLISHED 상태만 결제 가능
        if (invoice.getStatus() != InvoiceStatus.PUBLISHED) {
            throw new CoreException(ErrorType.INVOICE_NOT_PUBLISHED);
        }

        // 3. 청구서-거래처 소유 관계 검증
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));
        if (invoice.getClient() == null || !invoice.getClient().getId().equals(clientId)) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        // 4. 결제 저장 (invoice_id unique 제약으로 중복 결제 DB 레벨 차단 + paymentCode 충돌 재시도)
        int maxRetries = 3;
        Payment payment = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                String paymentCode = generateCode("PAY");
                payment = Payment.create(invoice, client, invoice.getDeal(), request.getPaymentMethod(), paymentCode);
                paymentRepository.saveAndFlush(payment);
                break;
            } catch (DataIntegrityViolationException e) {
                if (isInvoiceIdViolation(e)) {
                    // invoice_id unique 위반 = 이미 결제된 청구서
                    throw new CoreException(ErrorType.ALREADY_PAID);
                }
                if (!isPaymentCodeViolation(e)) throw e;
                if (i == maxRetries - 1) throw new CoreException(ErrorType.PAYMENT_CODE_OVERFLOW);
                // paymentCode 충돌만 재시도
            }
        }

        // 5. 청구서 상태 PAID로 변경
        invoice.paid();

        return PaymentResponse.from(payment);
    }

    /**
     * 결제 단건 조회
     */
    public PaymentResponse getPayment(Long paymentId, Long clientId) {

        Payment payment = paymentRepository.findByIdAndClientId(paymentId, clientId)
                .orElseThrow(() -> new CoreException(ErrorType.PAYMENT_NOT_FOUND)); // 불일치도 동일하게 NOT_FOUND

        return PaymentResponse.from(payment);
    }

    /**
     * 결제 목록 조회 (거래처별)
     */
    public List<PaymentListResponse> getPaymentsByClient(Long clientId) {
        return paymentRepository.findAllByClient_Id(clientId).stream()
                .map(PaymentListResponse::from)
                .toList();
    }

    /**
     * 결제 목록 조회 (전체)
     */
    public List<PaymentListResponse> getPayments() {
        return paymentRepository.findAll().stream()
                .map(PaymentListResponse::from)
                .toList();
    }

    // PAY-20260223-001 형식으로 코드 생성
    private String generateCode(String prefix) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String todayPrefix = prefix + "-" + date + "-";

        int nextSeq = paymentRepository.findMaxSuffixByPrefix(todayPrefix)
                .map(max -> max + 1)
                .orElse(1);

        if (nextSeq > 999) {
            throw new CoreException(ErrorType.PAYMENT_CODE_OVERFLOW);
        }

        return todayPrefix + String.format("%03d", nextSeq);
    }

    // invoice_id unique 제약 위반 여부 (중복 결제)
    private boolean isInvoiceIdViolation(DataIntegrityViolationException e) {
        String constraint = extractConstraintName(e);
        return "uk_payment_invoice_id".equals(constraint);
    }

    // payment_code unique 제약 위반 여부만 판별
    private boolean isPaymentCodeViolation(DataIntegrityViolationException e) {
        String constraint = extractConstraintName(e);
        return "uk_payment_code".equals(constraint);
    }

    // 1. Hibernate ConstraintViolationException에서 제약 이름 추출 시도
    // 2. 실패 시 MariaDB 메시지 패턴 파싱 (fallback)
    // 1. Hibernate ConstraintViolationException에서 제약 이름 추출 시도
    // 2. 실패 시 MariaDB 메시지 패턴 파싱 (fallback)
    private String extractConstraintName(DataIntegrityViolationException e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException cve
                    && cve.getConstraintName() != null) {
                return cve.getConstraintName().toLowerCase();
            }
            current = current.getCause();
        }

        // fallback: cause 체인에서 MariaDB "Duplicate entry '...' for key 'constraint_name'" 패턴 검색
        current = e;
        while (current != null) {
            String msg = current.getMessage() != null ? current.getMessage().toLowerCase() : "";
            int start = msg.indexOf("for key '");
            if (start != -1) {
                start += "for key '".length();
                int end = msg.indexOf("'", start);
                if (end != -1) {
                    return msg.substring(start, end);
                }
            }
            current = current.getCause();
        }
        return "";
    }
}
