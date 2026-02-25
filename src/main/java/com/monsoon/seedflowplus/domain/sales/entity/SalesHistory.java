package com.monsoon.seedflowplus.domain.sales.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.pipeline.enums.ActionType;
import com.monsoon.seedflowplus.domain.pipeline.enums.PipelineStage;
import com.monsoon.seedflowplus.domain.pipeline.enums.PipelineType;
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
                @Index(name = "idx_pipeline_ref_dt", columnList = "pipeline_type, ref_id, action_datetime"),
                @Index(name = "idx_client_dt",       columnList = "client_id, action_datetime"),
                @Index(name = "idx_actor_dt",        columnList = "actor_emp_id, action_datetime")
        }
)
public class SalesHistory extends BaseCreateEntity {

    // 문서 종류 (RFQ / QUO / CNT / ORD / STMT / INV / PAY)
    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_type", nullable = false, length = 10)
    private PipelineType pipelineType;

    // 문서 PK (pipelineType과 조합하여 원본 문서 식별)
    @Column(nullable = false)
    private Long refId;

    // 외부 노출용 문서 코드 (예: RFQ-20260225-001)
    @Column(nullable = false, length = 30)
    private String targetCode;

    // 변경 전 공통 단계 (최초 생성 시 null)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PipelineStage fromStage;

    // 변경 후 공통 단계
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PipelineStage toStage;

    // 변경 전 문서별 상태 (DocumentStatus 구현체의 name() 값, 최초 생성 시 null)
    // pipelineType에 따른 허용값 검증은 서비스 레이어에서 수행
    @Column(length = 30)
    private String fromStatus;

    // 변경 후 문서별 상태 (DocumentStatus 구현체의 name() 값)
    @Column(nullable = false, length = 30)
    private String toStatus;

    // 수행된 액션 종류
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ActionType actionType;

    // 행위자 타입 (EMP / ADMIN / CLIENT / SYSTEM)
    // APPROVE·REJECT 액션 시 관리자/거래처 구분에 활용
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ActorType actorType;

    // 액션 발생 시각
    @Column(nullable = false)
    private LocalDateTime actionDatetime;

    // 액션을 수행한 직원 (시스템 처리 또는 거래처 액션 시 null 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_emp_id")
    private Employee actorEmp;

    // 히스토리 대상 고객사
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Builder
    public SalesHistory(PipelineType pipelineType, Long refId, String targetCode,
                        PipelineStage fromStage, PipelineStage toStage,
                        String fromStatus, String toStatus,
                        ActionType actionType, ActorType actorType,
                        LocalDateTime actionDatetime, Employee actorEmp, Client client) {
        this.pipelineType = pipelineType;
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
