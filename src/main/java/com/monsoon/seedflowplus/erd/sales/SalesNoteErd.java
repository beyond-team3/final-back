package com.monsoon.seedflowplus.erd.sales;

import com.monsoon.seedflowplus.erd.account.EmployeeErd;
import com.monsoon.seedflowplus.erd.client.ClientErd;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
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
@Table(name = "tbl_sales_note")
public class SalesNoteErd {

    @Id
    @Column(name = "note_id")
    private Long noteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeErd employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientErd client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private SalesHistoryErd salesHistory;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Lob
    @Column(name = "ai_summary", nullable = false)
    private String aiSummary;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
