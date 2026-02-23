package com.monsoon.seedflowplus.erd.quotation;

import com.monsoon.seedflowplus.erd.account.EmployeeErd;
import com.monsoon.seedflowplus.erd.client.ClientErd;
import com.monsoon.seedflowplus.erd.request.RequestHeaderErd;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbl_quotation_header")
public class QuotationHeaderErd {

    @Id
    @Column(name = "quotation_id")
    private Long quotationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private RequestHeaderErd request;

    @Column(name = "quotation_status", nullable = false)
    private String quotationStatus;

    @Column(name = "total_fee", nullable = false)
    private Integer totalFee;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeErd employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientErd client;

    @Lob
    @Column(name = "note")
    private String note;
}
