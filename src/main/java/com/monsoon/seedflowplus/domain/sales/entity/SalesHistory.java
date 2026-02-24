package com.monsoon.seedflowplus.domain.sales.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import com.monsoon.seedflowplus.domain.account.Client;
import com.monsoon.seedflowplus.domain.account.Employee;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "sales_history_id"))
@Table(
        name = "tbl_sales_history",
        indexes = {
                @Index(name = "idx_type_ref_dt", columnList = "doc_type, ref_id, action_datetime"),
                @Index(name = "idx_client_dt", columnList = "client_id, action_datetime"),
                @Index(name = "idx_actor_dt", columnList = "actor_emp_id, action_datetime")
        }
)
public class SalesHistory extends BaseCreateEntity {

    // 문서 종류(RFQ/QUO/CNT/ORD/STMT/INV)
    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 20)
    private DocType docType;

    // 문서 PK를 담는 참조 ID (docType과 조합)
    @Column(nullable = false)
    private Long refId;

    // 외부 노출용 문서 코드
    @Column(nullable = false, length = 30)
    private String targetCode;

    // 변경 전 단계
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SalesStage fromStage;

    // 변경 후 단계
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SalesStage toStage;

    // 변경 전 상태(문서별 정책 문자열)
    @Column(length = 30)
    private String fromStatus;

    // 변경 후 상태(문서별 정책 문자열)
    @Column(nullable = false, length = 30)
    private String toStatus;

    // 수행된 액션 종류
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SalesActionType actionType;

    // 행위자 타입(직원/관리자/고객/시스템)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ActorType actorType;

    // 액션이 발생한 시각
    @Column(nullable = false)
    private LocalDateTime actionDatetime;

    // 액션을 수행한 직원(시스템 처리 시 null 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_emp_id")
    private Employee actorEmp;

    // 히스토리 대상 고객사
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Builder
    public SalesHistory(DocType docType, Long refId, String targetCode, SalesStage fromStage,
                        SalesStage toStage, String fromStatus, String toStatus,
                        SalesActionType actionType, ActorType actorType, LocalDateTime actionDatetime,
                        Employee actorEmp, Client client) {
        this.docType = docType;
        this.refId = refId;
        this.targetCode = targetCode;
        this.fromStage = fromStage;
        this.toStage = toStage;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actionType = actionType;
        this.actorType = actorType;
        this.actionDatetime = actionDatetime;
        this.actorEmp = actorEmp;
        this.client = client;
    }
}
