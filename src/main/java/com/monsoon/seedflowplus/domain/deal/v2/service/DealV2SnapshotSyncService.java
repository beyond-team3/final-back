package com.monsoon.seedflowplus.domain.deal.v2.service;

import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.DocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.entity.QDocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.DocumentSummaryRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealV2SnapshotSyncService {

    private final DocumentSummaryRepository documentSummaryRepository;

    @Transactional
    public void recalculate(SalesDeal deal) {
        QDocumentSummary documentSummary = QDocumentSummary.documentSummary;
        List<DocumentSummary> documents = StreamSupport.stream(
                        documentSummaryRepository.findAll(
                                documentSummary.dealId.eq(deal.getId()),
                                Sort.by(Sort.Order.desc("createdAt"))
                        ).spliterator(),
                        false
                )
                .toList();

        if (documents.isEmpty()) {
            return;
        }

        DocumentSummary representative = documents.stream()
                .max(documentComparator())
                .orElse(documents.get(0));

        LocalDateTime lastActivityAt = documents.stream()
                .map(DocumentSummary::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElseGet(LocalDateTime::now);

        deal.updateSnapshot(
                mapStage(representative),
                representative.getStatus(),
                representative.getDocType(),
                representative.getDocId(),
                representative.getDocCode(),
                lastActivityAt
        );
    }

    private Comparator<DocumentSummary> documentComparator() {
        return Comparator.comparingInt((DocumentSummary doc) -> stagePriority(doc.getDocType()))
                .thenComparingInt(this::statusPriority)
                .thenComparing(DocumentSummary::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
    }

    private int stagePriority(DealType docType) {
        return switch (docType) {
            case RFQ -> 1;
            case QUO -> 2;
            case CNT -> 3;
            case ORD -> 4;
            case STMT -> 5;
            case INV -> 6;
            case PAY -> 7;
        };
    }

    private int statusPriority(DocumentSummary document) {
        String status = document.getStatus();
        return switch (document.getDocType()) {
            case RFQ -> switch (status) {
                case "PENDING", "REVIEWING" -> 5;
                case "COMPLETED" -> 4;
                case "DELETED" -> 1;
                default -> 0;
            };
            case QUO -> switch (status) {
                case "WAITING_CLIENT" -> 7;
                case "WAITING_ADMIN" -> 6;
                case "FINAL_APPROVED", "WAITING_CONTRACT" -> 5;
                case "COMPLETED" -> 4;
                case "REJECTED_CLIENT", "REJECTED_ADMIN" -> 3;
                case "DELETED" -> 2;
                case "EXPIRED" -> 1;
                default -> 0;
            };
            case CNT -> switch (status) {
                case "WAITING_CLIENT" -> 7;
                case "WAITING_ADMIN" -> 6;
                case "COMPLETED", "ACTIVE_CONTRACT" -> 5;
                case "REJECTED_CLIENT", "REJECTED_ADMIN" -> 3;
                case "DELETED" -> 2;
                case "EXPIRED" -> 1;
                default -> 0;
            };
            case ORD -> switch (status) {
                case "CONFIRMED" -> 6;
                case "PENDING" -> 5;
                case "CANCELED" -> 1;
                default -> 0;
            };
            case STMT -> switch (status) {
                case "ISSUED" -> 5;
                case "CANCELED" -> 1;
                default -> 0;
            };
            case INV -> switch (status) {
                case "PAID" -> 6;
                case "PUBLISHED" -> 5;
                case "DRAFT" -> 4;
                case "CANCELED" -> 1;
                default -> 0;
            };
            case PAY -> switch (status) {
                case "COMPLETED" -> 6;
                case "PENDING" -> 5;
                case "FAILED" -> 1;
                default -> 0;
            };
        };
    }

    private DealStage mapStage(DocumentSummary document) {
        String status = document.getStatus();
        return switch (document.getDocType()) {
            case RFQ -> switch (status) {
                case "PENDING" -> DealStage.CREATED;
                case "REVIEWING" -> DealStage.IN_PROGRESS;
                case "COMPLETED" -> DealStage.APPROVED;
                default -> DealStage.CANCELED;
            };
            case QUO -> switch (status) {
                case "WAITING_ADMIN" -> DealStage.PENDING_ADMIN;
                case "REJECTED_ADMIN" -> DealStage.REJECTED_ADMIN;
                case "WAITING_CLIENT", "FINAL_APPROVED" -> DealStage.PENDING_CLIENT;
                case "REJECTED_CLIENT" -> DealStage.REJECTED_CLIENT;
                case "WAITING_CONTRACT", "COMPLETED" -> DealStage.APPROVED;
                case "EXPIRED" -> DealStage.EXPIRED;
                default -> DealStage.CANCELED;
            };
            case CNT -> switch (status) {
                case "WAITING_ADMIN" -> DealStage.PENDING_ADMIN;
                case "REJECTED_ADMIN" -> DealStage.REJECTED_ADMIN;
                case "WAITING_CLIENT" -> DealStage.PENDING_CLIENT;
                case "REJECTED_CLIENT" -> DealStage.REJECTED_CLIENT;
                case "COMPLETED" -> DealStage.APPROVED;
                case "ACTIVE_CONTRACT" -> DealStage.CONFIRMED;
                case "EXPIRED" -> DealStage.EXPIRED;
                default -> DealStage.CANCELED;
            };
            case ORD -> switch (status) {
                case "PENDING" -> DealStage.IN_PROGRESS;
                case "CONFIRMED" -> DealStage.CONFIRMED;
                default -> DealStage.CANCELED;
            };
            case STMT -> "ISSUED".equals(status) ? DealStage.ISSUED : DealStage.CANCELED;
            case INV -> switch (status) {
                case "DRAFT" -> DealStage.CREATED;
                case "PUBLISHED" -> DealStage.ISSUED;
                case "PAID" -> DealStage.PAID;
                default -> DealStage.CANCELED;
            };
            case PAY -> switch (status) {
                case "PENDING" -> DealStage.IN_PROGRESS;
                case "COMPLETED" -> DealStage.PAID;
                default -> DealStage.CANCELED;
            };
        };
    }
}
