package com.monsoon.seedflowplus.domain.sales.request.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "id", column = @Column(name = "rfq_id"))
@Table(name = "tbl_request_quotation_header")
public class QuotationRequestHeader extends BaseModifyEntity {

    @Column(name = "request_code", unique = true)
    private String requestCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client; // 작성자

    @Column(name = "requirements")
    private String requirements; // 요구사항

    @Column(name = "request_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private QuotationRequestStatus status; // 상태 (대기, 검토, 완료)

    @OneToMany(mappedBy = "quotationRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuotationRequestDetail> items = new ArrayList<>();

    private QuotationRequestHeader(Client client, String requirements) {
        this.client = client;
        this.requirements = requirements;
        this.status = QuotationRequestStatus.PENDING;
    }

    public static QuotationRequestHeader create(Client client, String requirements) {
        return new QuotationRequestHeader(client, requirements);
    }

    public void updateRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public void addItem(QuotationRequestDetail item) {
        if (item == null) {
            throw new IllegalArgumentException("아이템은 null일 수 없습니다.");
        }
        this.items.add(item);
        item.setQuotationRequest(this);
    }

    public void delete() {
        this.status = QuotationRequestStatus.DELETED;
    }
}
