package com.monsoon.seedflowplus.domain.deal.core.service;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.deal.common.DealPaginationConstants;
import com.monsoon.seedflowplus.domain.deal.common.error.DealException;
import com.monsoon.seedflowplus.domain.deal.common.error.DealErrorCode;
import com.monsoon.seedflowplus.domain.deal.core.dto.response.SalesDealListItemDto;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealSearchCondition;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
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

    private final SalesDealRepository salesDealRepository;

    public Page<SalesDealListItemDto> getDealsForCurrentUser(
            SalesDealSearchCondition cond,
            Pageable pageable,
            CustomUserDetails user
    ) {
        validateDateRange(cond);

        SalesDealSearchCondition scopedCondition = enforceScope(cond, user);
        Pageable cappedPageable = capPageSize(pageable);

        return salesDealRepository.searchDeals(scopedCondition, cappedPageable)
                .map(this::toListItemDto);
    }

    private SalesDealSearchCondition enforceScope(SalesDealSearchCondition cond, CustomUserDetails user) {
        if (user == null || user.getRole() == null) {
            throw new AccessDeniedException("사용자 권한 정보가 없습니다.");
        }

        SalesDealSearchCondition base = cond == null ? SalesDealSearchCondition.builder().build() : cond;

        if (user.getRole() == Role.ADMIN) {
            return base;
        }

        if (user.getRole() == Role.SALES_REP) {
            if (user.getEmployeeId() == null) {
                throw new AccessDeniedException("영업사원 사용자에 employeeId가 없습니다.");
            }
            return base.toBuilder()
                    .ownerEmpId(user.getEmployeeId())
                    .build();
        }

        if (user.getRole() == Role.CLIENT) {
            if (user.getClientId() == null) {
                throw new AccessDeniedException("거래처 사용자에 clientId가 없습니다.");
            }
            return base.toBuilder()
                    .clientId(user.getClientId())
                    .build();
        }

        throw new AccessDeniedException("허용되지 않은 역할입니다: " + user.getRole());
    }

    private void validateDateRange(SalesDealSearchCondition cond) {
        if (cond == null) {
            return;
        }

        LocalDateTime fromAt = cond.getFromAt();
        LocalDateTime toAt = cond.getToAt();
        if (fromAt != null && toAt != null && fromAt.isAfter(toAt)) {
            throw new DealException(DealErrorCode.INVALID_DATE_RANGE);
        }
    }

    private Pageable capPageSize(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, DealPaginationConstants.MAX_PAGE_SIZE);
        }

        int requestedSize = pageable.getPageSize();
        if (requestedSize <= DealPaginationConstants.MAX_PAGE_SIZE) {
            return pageable;
        }

        return PageRequest.of(pageable.getPageNumber(), DealPaginationConstants.MAX_PAGE_SIZE, pageable.getSort());
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
