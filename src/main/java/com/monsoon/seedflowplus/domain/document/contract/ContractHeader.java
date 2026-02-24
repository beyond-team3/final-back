package com.monsoon.seedflowplus.domain.document.contract;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import com.monsoon.seedflowplus.domain.account.Client;
import com.monsoon.seedflowplus.domain.account.Employee;
import com.monsoon.seedflowplus.domain.document.quotation.QuotationHeader;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "cnt_id")),
        @AttributeOverride(name = "createdAt", column = @Column(name = "issue_date"))
})
@Table(name = "tbl_contract_header")
public class ContractHeader extends BaseModifyEntity {

    @Column(name = "contract_code", unique = true)
    private String contractCode; // id: CT-1771728490492

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quo_id")
    private QuotationHeader quotation; // 참조 견적서

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_code", referencedColumnName = "employee_code")
    private Employee author; // 작성자

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ContractStatus status; // 계약서 상태

    @Column(name = "total_amount")
    private BigDecimal totalAmount; // 총 가격

    @Column(name = "start_date")
    private LocalDate startDate; // 계약 시작 날짜

    @Column(name = "end_date")
    private LocalDate endDate; // 계약 종료 날짜

    @Column(name = "billing_cycle")
    @Enumerated(EnumType.STRING)
    private BillingCycle billingCycle; // 청구 주기

    @Column(name = "special_terms", columnDefinition = "TEXT")
    private String specialTerms; // 특약 사항

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo; // 비고

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractDetail> items = new ArrayList<>(); // 계약 작물 목록
}
