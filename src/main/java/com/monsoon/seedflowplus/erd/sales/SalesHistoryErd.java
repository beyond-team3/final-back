package com.monsoon.seedflowplus.erd;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "tbl_sales_history")
public class SalesHistoryErd {

    @Id
    @Column(name = "sales_history_id")
    private Long salesHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_key", nullable = false)
    private ClientErd client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_key", nullable = false)
    private EmployeeErd employee;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "sales_title", nullable = false)
    private String salesTitle;

    @Column(name = "sales_status", nullable = false)
    private String salesStatus;

    @Column(name = "Field")
    private String field;
}
