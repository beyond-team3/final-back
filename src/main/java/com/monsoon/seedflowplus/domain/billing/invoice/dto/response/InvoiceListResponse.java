package com.monsoon.seedflowplus.domain.billing.invoice.dto.response;

import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class InvoiceListResponse {

    private Long invoiceId;
    private String invoiceCode;
    private Long contractId;
    private String contractCode;   // 추가: 계약 코드 (프론트 리스트 렌더링용)
    private Long clientId;
    private String clientName;     // 추가: 거래처명 (프론트 리스트 렌더링용)
    private LocalDate invoiceDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
    private LocalDateTime createdAt;

    public static InvoiceListResponse from(Invoice invoice) {
        // contract 정보 조회 (N+1 주의: InvoiceService에서 fetch join 권장)
        String contractCode = null;
        if (invoice.getContractId() != null) {
            // Invoice 엔티티에 contract 연관관계가 있는 경우 직접 접근
            // 없는 경우 InvoiceService에서 contractRepository로 조회 후 별도 from() 오버로드 사용
            contractCode = null; // 아래 from(invoice, contractCode, clientName) 오버로드 사용 권장
        }

        String clientName = invoice.getClient() != null
                ? invoice.getClient().getClientName()
                : null;

        return InvoiceListResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceCode(invoice.getInvoiceCode())
                .contractId(invoice.getContractId())
                .contractCode(contractCode)
                .clientId(invoice.getClient() != null ? invoice.getClient().getId() : null)
                .clientName(clientName)
                .invoiceDate(invoice.getInvoiceDate())
                .startDate(invoice.getStartDate())
                .endDate(invoice.getEndDate())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    /**
     * contractCode를 별도로 전달받는 오버로드
     * InvoiceService에서 ContractRepository로 조회한 뒤 사용
     */
    public static InvoiceListResponse from(Invoice invoice, String contractCode) {
        String clientName = invoice.getClient() != null
                ? invoice.getClient().getClientName()
                : null;

        return InvoiceListResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceCode(invoice.getInvoiceCode())
                .contractId(invoice.getContractId())
                .contractCode(contractCode)
                .clientId(invoice.getClient() != null ? invoice.getClient().getId() : null)
                .clientName(clientName)
                .invoiceDate(invoice.getInvoiceDate())
                .startDate(invoice.getStartDate())
                .endDate(invoice.getEndDate())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}