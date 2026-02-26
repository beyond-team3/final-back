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

        // 3. 이미 결제된 청구서 방지
        paymentRepository.findByInvoice_Id(request.getInvoiceId())
                .ifPresent(p -> { throw new CoreException(ErrorType.ALREADY_PAID); });

        // 4. 거래처 조회
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));

        // 5. 결제 저장 (paymentCode unique 충돌 시 최대 3회 재시도)
        int maxRetries = 3;
        Payment payment = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                String paymentCode = generateCode("PAY");
                payment = Payment.create(invoice, client, request.getPaymentMethod(), paymentCode);
                paymentRepository.saveAndFlush(payment);
                break;
            } catch (DataIntegrityViolationException e) {
                if (!isPaymentCodeViolation(e) || i == maxRetries - 1) throw e;
            }
        }

        // 6. 청구서 상태 PAID로 변경
        if (invoice.getStatus() != InvoiceStatus.PUBLISHED) {
            throw new CoreException(ErrorType.ALREADY_PAID);
        }
        invoice.paid();

        return PaymentResponse.from(payment);
    }

    /**
     * 결제 단건 조회
     */
    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CoreException(ErrorType.PAYMENT_NOT_FOUND));
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

        return todayPrefix + String.format("%03d", nextSeq);
    }

    // payment_code 유니크 제약 위반 여부 판별
    private boolean isPaymentCodeViolation(DataIntegrityViolationException e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("payment_code") || msg.contains("uk_") || msg.contains("unique");
    }
}