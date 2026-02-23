package com.monsoon.seedflowplus.erd.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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
@Table(name = "tbl_pdf_storage")
public class PdfStorageErd {

    @Id
    @Column(name = "pdf_id")
    private Long pdfId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Lob
    @Column(name = "file_data", nullable = false)
    private byte[] fileData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
