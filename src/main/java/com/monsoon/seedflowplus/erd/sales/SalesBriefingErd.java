package com.monsoon.seedflowplus.erd;

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
@Table(name = "tbl_sales_briefing")
public class SalesBriefingErd {

    @Id
    @Column(name = "briefing_id")
    private Long briefingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientErd client;

    @Lob
    @Column(name = "status_change", nullable = false)
    private String statusChange;

    @Lob
    @Column(name = "long_term_pattern", nullable = false)
    private String longTermPattern;

    @Lob
    @Column(name = "strategy_suggestion", nullable = false)
    private String strategySuggestion;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
