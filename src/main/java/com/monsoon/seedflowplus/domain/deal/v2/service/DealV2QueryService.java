package com.monsoon.seedflowplus.domain.deal.v2.service;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.deal.common.DealPaginationConstants;
import com.monsoon.seedflowplus.domain.deal.core.entity.DocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.entity.QDocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.DocumentSummaryRepository;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealSearchCondition;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealDetailDto;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealDocumentSummaryDto;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealSnapshotDto;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealSummaryDto;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import com.querydsl.core.types.Predicate;
import java.util.List;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealV2QueryService {

    private final SalesDealRepository salesDealRepository;
    private final DocumentSummaryRepository documentSummaryRepository;

    public Page<DealSummaryDto> getDeals(
            SalesDealSearchCondition condition,
            Pageable pageable,
            CustomUserDetails userDetails
    ) {
        validateUserDetails(userDetails);
        Pageable cappedPageable = capPageSize(pageable);

        return salesDealRepository.searchDeals(enforceScope(condition, userDetails), cappedPageable)
                .map(this::toSummaryDto);
    }

    public DealDetailDto getDeal(Long dealId, CustomUserDetails userDetails) {
        validateUserDetails(userDetails);

        SalesDeal deal = salesDealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("deal not found: " + dealId));
        assertAccessible(deal, userDetails);

        return toDetailDto(deal);
    }

    public List<DealDocumentSummaryDto> getDealDocuments(Long dealId, CustomUserDetails userDetails) {
        validateUserDetails(userDetails);

        SalesDeal deal = salesDealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("deal not found: " + dealId));
        assertAccessible(deal, userDetails);

        QDocumentSummary documentSummary = QDocumentSummary.documentSummary;
        Predicate predicate = documentSummary.dealId.eq(dealId);

        return StreamSupport.stream(
                        documentSummaryRepository.findAll(predicate, Sort.by(Sort.Order.desc("createdAt"))).spliterator(),
                        false
                )
                .map(this::toDocumentSummaryDto)
                .toList();
    }

    private SalesDealSearchCondition enforceScope(SalesDealSearchCondition condition, CustomUserDetails userDetails) {
        SalesDealSearchCondition base = condition == null
                ? SalesDealSearchCondition.builder().build()
                : condition;

        if (userDetails.getRole() == Role.ADMIN) {
            return base;
        }

        if (userDetails.getRole() == Role.SALES_REP) {
            return base.toBuilder()
                    .ownerEmpId(userDetails.getEmployeeId())
                    .build();
        }

        if (userDetails.getRole() == Role.CLIENT) {
            return base.toBuilder()
                    .clientId(userDetails.getClientId())
                    .build();
        }

        throw new AccessDeniedException("허용되지 않은 역할입니다: " + userDetails.getRole());
    }

    private void validateUserDetails(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getRole() == null) {
            throw new AccessDeniedException("사용자 권한 정보가 없습니다.");
        }

        if (userDetails.getRole() == Role.SALES_REP && userDetails.getEmployeeId() == null) {
            throw new AccessDeniedException("영업사원 사용자에 employeeId가 없습니다.");
        }

        if (userDetails.getRole() == Role.CLIENT && userDetails.getClientId() == null) {
            throw new AccessDeniedException("거래처 사용자에 clientId가 없습니다.");
        }
    }

    private void assertAccessible(SalesDeal deal, CustomUserDetails userDetails) {
        if (userDetails.getRole() == Role.ADMIN) {
            return;
        }

        if (userDetails.getRole() == Role.SALES_REP) {
            Long ownerEmpId = deal.getOwnerEmp() != null ? deal.getOwnerEmp().getId() : null;
            if (ownerEmpId != null && ownerEmpId.equals(userDetails.getEmployeeId())) {
                return;
            }
        }

        if (userDetails.getRole() == Role.CLIENT) {
            Long clientId = deal.getClient() != null ? deal.getClient().getId() : null;
            if (clientId != null && clientId.equals(userDetails.getClientId())) {
                return;
            }
        }

        throw new AccessDeniedException("해당 deal에 접근할 수 없습니다.");
    }

    private Pageable capPageSize(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, DealPaginationConstants.MAX_PAGE_SIZE);
        }

        if (pageable.getPageSize() <= DealPaginationConstants.MAX_PAGE_SIZE) {
            return pageable;
        }

        return PageRequest.of(pageable.getPageNumber(), DealPaginationConstants.MAX_PAGE_SIZE, pageable.getSort());
    }

    private DealSummaryDto toSummaryDto(SalesDeal deal) {
        return DealSummaryDto.builder()
                .dealId(deal.getId())
                .dealCode(null)
                .dealTitle(null)
                .clientId(deal.getClient() != null ? deal.getClient().getId() : null)
                .clientName(deal.getClient() != null ? deal.getClient().getClientName() : null)
                .ownerEmpId(deal.getOwnerEmp() != null ? deal.getOwnerEmp().getId() : null)
                .ownerEmpName(deal.getOwnerEmp() != null ? deal.getOwnerEmp().getEmployeeName() : null)
                .snapshot(toSnapshotDto(deal))
                .closedAt(deal.getClosedAt())
                .build();
    }

    private DealDetailDto toDetailDto(SalesDeal deal) {
        return DealDetailDto.builder()
                .dealId(deal.getId())
                .dealCode(null)
                .dealTitle(null)
                .clientId(deal.getClient() != null ? deal.getClient().getId() : null)
                .clientName(deal.getClient() != null ? deal.getClient().getClientName() : null)
                .ownerEmpId(deal.getOwnerEmp() != null ? deal.getOwnerEmp().getId() : null)
                .ownerEmpName(deal.getOwnerEmp() != null ? deal.getOwnerEmp().getEmployeeName() : null)
                .summaryMemo(deal.getSummaryMemo())
                .snapshot(toSnapshotDto(deal))
                .createdAt(deal.getCreatedAt())
                .updatedAt(deal.getUpdatedAt())
                .closedAt(deal.getClosedAt())
                .build();
    }

    private DealSnapshotDto toSnapshotDto(SalesDeal deal) {
        return DealSnapshotDto.builder()
                .currentStage(deal.getCurrentStage())
                .currentStatus(deal.getCurrentStatus())
                .representativeDocumentType(deal.getLatestDocType())
                .representativeDocumentId(deal.getLatestRefId())
                .lifecycleStatus(null)
                .approvalStatus(null)
                .documentRole(null)
                .lastActivityAt(deal.getLastActivityAt())
                .build();
    }

    private DealDocumentSummaryDto toDocumentSummaryDto(DocumentSummary documentSummary) {
        return DealDocumentSummaryDto.builder()
                .surrogateId(documentSummary.getSurrogateId())
                .documentType(documentSummary.getDocType())
                .documentId(documentSummary.getDocId())
                .documentCode(documentSummary.getDocCode())
                .amount(documentSummary.getAmount())
                .expiredDate(documentSummary.getExpiredDate())
                .currentStatus(documentSummary.getStatus())
                .createdAt(documentSummary.getCreatedAt())
                .build();
    }
}
