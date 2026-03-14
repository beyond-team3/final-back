package com.monsoon.seedflowplus.domain.deal.v2.service;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.DocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.entity.QDocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.DocumentSummaryRepository;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationRepository;
import com.monsoon.seedflowplus.domain.schedule.dto.response.ScheduleItemDto;
import com.monsoon.seedflowplus.domain.schedule.query.ScheduleQueryService;
import com.monsoon.seedflowplus.domain.schedule.query.ScheduleSearchCondition;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealV2ContextQueryService {

    private final SalesDealRepository salesDealRepository;
    private final DocumentSummaryRepository documentSummaryRepository;
    private final NotificationRepository notificationRepository;
    private final ScheduleQueryService scheduleQueryService;

    public Page<Notification> getDealNotifications(Long dealId, Pageable pageable, CustomUserDetails userDetails) {
        SalesDeal deal = loadAccessibleDeal(dealId, userDetails);
        Map<DealType, List<Long>> docIds = loadDocumentIdsByType(dealId);
        return notificationRepository.findDealContextNotifications(
                userDetails.getUserId(),
                deal.getId(),
                safeIds(docIds.get(DealType.QUO)),
                safeIds(docIds.get(DealType.CNT)),
                safeIds(docIds.get(DealType.ORD)),
                safeIds(docIds.get(DealType.INV)),
                safeIds(docIds.get(DealType.STMT)),
                pageable
        );
    }

    public List<ScheduleItemDto> getDealSchedules(
            Long dealId,
            LocalDateTime from,
            LocalDateTime to,
            CustomUserDetails userDetails
    ) {
        SalesDeal deal = loadAccessibleDeal(dealId, userDetails);
        ScheduleSearchCondition condition = ScheduleSearchCondition.builder()
                .rangeStart(from)
                .rangeEnd(to)
                .ownerId(userDetails.getUserId())
                .dealId(deal.getId())
                .actorRole(userDetails.getRole())
                .actorUserId(userDetails.getUserId())
                .actorClientId(userDetails.getClientId())
                .includePersonal(false)
                .includeDeal(true)
                .build();
        return scheduleQueryService.getUnifiedSchedules(condition);
    }

    private SalesDeal loadAccessibleDeal(Long dealId, CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUserId() == null || userDetails.getRole() == null) {
            throw new AccessDeniedException("사용자 권한 정보가 없습니다.");
        }

        SalesDeal deal = salesDealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("deal not found: " + dealId));

        if (userDetails.getRole() == Role.ADMIN) {
            return deal;
        }
        if (userDetails.getRole() == Role.SALES_REP
                && deal.getOwnerEmp() != null
                && deal.getOwnerEmp().getId() != null
                && deal.getOwnerEmp().getId().equals(userDetails.getEmployeeId())) {
            return deal;
        }
        if (userDetails.getRole() == Role.CLIENT
                && deal.getClient() != null
                && deal.getClient().getId() != null
                && deal.getClient().getId().equals(userDetails.getClientId())) {
            return deal;
        }

        throw new AccessDeniedException("해당 deal에 접근할 수 없습니다.");
    }

    private Map<DealType, List<Long>> loadDocumentIdsByType(Long dealId) {
        QDocumentSummary documentSummary = QDocumentSummary.documentSummary;
        List<DocumentSummary> documents = StreamSupport.stream(
                        documentSummaryRepository.findAll(
                                documentSummary.dealId.eq(dealId),
                                Sort.by(Sort.Order.desc("createdAt"))
                        ).spliterator(),
                        false
                )
                .toList();

        Map<DealType, List<Long>> docIdsByType = new EnumMap<>(DealType.class);
        for (DealType dealType : DealType.values()) {
            docIdsByType.put(
                    dealType,
                    documents.stream()
                            .filter(document -> document.getDocType() == dealType)
                            .map(DocumentSummary::getDocId)
                            .toList()
            );
        }
        return docIdsByType;
    }

    private List<Long> safeIds(List<Long> ids) {
        return ids == null || ids.isEmpty() ? List.of(-1L) : ids;
    }
}
