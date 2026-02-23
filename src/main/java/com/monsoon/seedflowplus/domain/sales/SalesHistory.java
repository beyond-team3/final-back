package com.monsoon.seedflowplus.domain.sales;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "tbl_sales_history",
        indexes = {
                @Index(name = "idx_type_ref_dt", columnList = "pipeline_type, ref_id, action_datetime"),
                @Index(name = "idx_client_dt", columnList = "client_id, action_datetime")
        }
)
public class SalesHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id", nullable = false)
    private Long historyId;

    @Column(name = "pipeline_type", nullable = false, length = 10)
    private Enum pipelineType;

    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Column(name = "target_code", nullable = false, unique = true, length = 30)
    private String targetCode;

    @Column(name = "from_stage")
    private Enum fromStage;

    @Column(name = "to_stage", nullable = false)
    private Enum toStage;

    @Column(name = "from_status")
    private Enum fromStatus;

    @Column(name = "to_status", nullable = false)
    private Enum toStatus;

    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;

    @Column(name = "action_datetime", nullable = false)
    private LocalDateTime actionDatetime;

    @Column(name = "actor_emp_id")
    private Long actorEmpId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
