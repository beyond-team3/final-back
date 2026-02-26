package com.monsoon.seedflowplus.domain.deal.core.service;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.deal.core.dto.response.SalesDealListItemDto;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealSearchCondition;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesDealQueryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final SalesDealRepository salesDealRepository;

    public Page<SalesDealListItemDto> getDealsForCurrentUser(
            SalesDealSearchCondition cond,
            Pageable pageable,
            TempUser user
    ) {
        validateDateRange(cond);

        SalesDealSearchCondition scopedCondition = enforceScope(cond, user);
        Pageable cappedPageable = capPageSize(pageable);

        return salesDealRepository.searchDeals(scopedCondition, cappedPageable)
                .map(this::toListItemDto);
    }

    private SalesDealSearchCondition enforceScope(SalesDealSearchCondition cond, TempUser user) {
        if (user == null || user.role() == null) {
            throw new AccessDeniedException("사용자 권한 정보가 없습니다.");
        }

        SalesDealSearchCondition base = cond == null ? SalesDealSearchCondition.builder().build() : cond;

        if (user.role() == Role.ADMIN) {
            return base;
        }

        if (user.role() == Role.SALES_REP) {
            if (user.employeeId() == null) {
                throw new AccessDeniedException("영업사원 사용자에 employeeId가 없습니다.");
            }
            return base.toBuilder()
                    .ownerEmpId(user.employeeId())
                    .build();
        }

        if (user.role() == Role.CLIENT) {
            if (user.clientId() == null) {
                throw new AccessDeniedException("거래처 사용자에 clientId가 없습니다.");
            }
            return base.toBuilder()
                    .clientId(user.clientId())
                    .build();
        }

        throw new AccessDeniedException("허용되지 않은 역할입니다: " + user.role());
    }

    private void validateDateRange(SalesDealSearchCondition cond) {
        if (cond == null) {
            return;
        }

        LocalDateTime fromAt = cond.getFromAt();
        LocalDateTime toAt = cond.getToAt();
        if (fromAt != null && toAt != null && fromAt.isAfter(toAt)) {
            throw new IllegalArgumentException("fromAt은 toAt보다 늦을 수 없습니다.");
        }
    }

    private Pageable capPageSize(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, MAX_PAGE_SIZE);
        }

        int requestedSize = pageable.getPageSize();
        if (requestedSize <= MAX_PAGE_SIZE) {
            return pageable;
        }

        return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
    }

    private SalesDealListItemDto toListItemDto(SalesDeal deal) {
        return SalesDealListItemDto.builder()
                .dealId(deal.getId())
                .clientId(deal.getClient() != null ? deal.getClient().getId() : null)
                .clientName(deal.getClient() != null ? deal.getClient().getClientName() : null)
                .ownerEmpId(deal.getOwnerEmp() != null ? deal.getOwnerEmp().getId() : null)
                .ownerEmpName(deal.getOwnerEmp() != null ? deal.getOwnerEmp().getEmployeeName() : null)
                .currentStage(deal.getCurrentStage())
                .currentStatus(deal.getCurrentStatus())
                .latestDocType(deal.getLatestDocType())
                .latestRefId(deal.getLatestRefId())
                .latestTargetCode(deal.getLatestTargetCode())
                .lastActivityAt(deal.getLastActivityAt())
                .closedAt(deal.getClosedAt())
                .summaryMemo(deal.getSummaryMemo())
                .build();
    }
}
