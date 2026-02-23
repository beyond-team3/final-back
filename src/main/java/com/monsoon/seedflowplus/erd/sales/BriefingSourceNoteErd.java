package com.monsoon.seedflowplus.erd.sales;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "briefing_source_note")
public class BriefingSourceNoteErd {

    @Id
    @Column(name = "source_id")
    private Long sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private SalesNoteErd note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "briefing_id", nullable = false)
    private SalesBriefingErd briefing;
}
