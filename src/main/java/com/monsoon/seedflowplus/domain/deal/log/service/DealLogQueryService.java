package com.monsoon.seedflowplus.domain.deal.log.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogDetailDto;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogSummaryDto;
import com.monsoon.seedflowplus.domain.deal.log.entity.DealLogDetail;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.service.TempUser;
import com.monsoon.seedflowplus.domain.deal.log.entity.SalesDealLog;
import com.monsoon.seedflowplus.domain.deal.log.repository.DealLogDetailRepository;
import com.monsoon.seedflowplus.domain.deal.log.repository.SalesDealLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealLogQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final SalesDealLogRepository salesDealLogRepository;
    private final DealLogDetailRepository dealLogDetailRepository;

    public Page<DealLogSummaryDto> getTimelineByDeal(Long dealId, Pageable pageable, TempUser user) {
        Pageable resolvedPageable = resolveTimelinePageable(pageable);
        TempUser requiredUser = requireUser(user);

        return findByDealWithScope(dealId, resolvedPageable, requiredUser)
                .map(this::toSummaryDto);
    }

    public Page<DealLogSummaryDto> getTimelineByClient(Long clientId, Pageable pageable, TempUser user) {
        Pageable resolvedPageable = resolveTimelinePageable(pageable);
        TempUser requiredUser = requireUser(user);

        return findByClientWithScope(clientId, resolvedPageable, requiredUser)
                .map(this::toSummaryDto);
    }

    public Page<DealLogSummaryDto> getTimelineByDocument(DealType docType, Long refId, Pageable pageable, TempUser user) {
        Pageable resolvedPageable = resolveTimelinePageable(pageable);
        TempUser requiredUser = requireUser(user);

        return findByDocumentWithScope(docType, refId, resolvedPageable, requiredUser)
                .map(this::toSummaryDto);
    }

    public DealLogDetailDto getLogDetail(Long dealLogId, TempUser user) {
        TempUser requiredUser = requireUser(user);
        DealLogDetail detail = dealLogDetailRepository.findByDealLogId(dealLogId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.DEAL_LOG_DETAIL_NOT_FOUND,
                        "DealLogDetail을 찾을 수 없습니다. dealLogId=" + dealLogId
                ));

        SalesDealLog log = detail.getDealLog();
        if (!canAccess(requiredUser, log.getDeal().getOwnerEmp().getId(), log.getClient().getId())) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        return toDetailDto(detail);
    }

    private Pageable resolveTimelinePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE, SalesDealLogRepository.DEFAULT_TIMELINE_SORT);
        }
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return SalesDealLogRepository.withDefaultTimelineSort(pageable);
    }

    private DealLogSummaryDto toSummaryDto(SalesDealLog log) {
        return DealLogSummaryDto.builder()
                .dealLogId(log.getId())
                .docType(log.getDocType())
                .refId(log.getRefId())
                .targetCode(log.getTargetCode())
                .fromStage(log.getFromStage())
                .toStage(log.getToStage())
                .fromStatus(log.getFromStatus())
                .toStatus(log.getToStatus())
                .actionType(log.getActionType())
                .actionAt(log.getActionAt())
                .actorType(log.getActorType())
                .actorId(log.getActorId())
                .build();
    }

    private DealLogDetailDto toDetailDto(DealLogDetail detail) {
        return DealLogDetailDto.builder()
                .dealLogId(detail.getDealLog().getId())
                .reason(detail.getReason())
                .diffJson(detail.getDiffJson())
                .createdAt(detail.getCreatedAt())
                .build();
    }

    private Page<SalesDealLog> findByDealWithScope(Long dealId, Pageable pageable, TempUser user) {
        if (user.role() == Role.ADMIN) {
            return salesDealLogRepository.findByDealId(dealId, pageable);
        }
        if (user.role() == Role.SALES_REP) {
            if (user.employeeId() == null) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return salesDealLogRepository.findByDealIdAndOwnerEmpId(dealId, user.employeeId(), pageable);
        }
        if (user.role() == Role.CLIENT) {
            if (user.clientId() == null) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return salesDealLogRepository.findByDealIdAndClientId(dealId, user.clientId(), pageable);
        }
        throw new CoreException(ErrorType.ACCESS_DENIED);
    }

    private Page<SalesDealLog> findByClientWithScope(Long clientId, Pageable pageable, TempUser user) {
        if (user.role() == Role.ADMIN) {
            return salesDealLogRepository.findByClientId(clientId, pageable);
        }
        if (user.role() == Role.SALES_REP) {
            if (user.employeeId() == null) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return salesDealLogRepository.findByClientIdAndOwnerEmpId(clientId, user.employeeId(), pageable);
        }
        if (user.role() == Role.CLIENT) {
            if (user.clientId() == null || !user.clientId().equals(clientId)) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return salesDealLogRepository.findByClientId(clientId, pageable);
        }
        throw new CoreException(ErrorType.ACCESS_DENIED);
    }

    private Page<SalesDealLog> findByDocumentWithScope(DealType docType, Long refId, Pageable pageable, TempUser user) {
        if (user.role() == Role.ADMIN) {
            return salesDealLogRepository.findByDocTypeAndRefId(docType, refId, pageable);
        }
        if (user.role() == Role.SALES_REP) {
            if (user.employeeId() == null) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return salesDealLogRepository.findByDocTypeAndRefIdAndOwnerEmpId(docType, refId, user.employeeId(), pageable);
        }
        if (user.role() == Role.CLIENT) {
            if (user.clientId() == null) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return salesDealLogRepository.findByDocTypeAndRefIdAndClientId(docType, refId, user.clientId(), pageable);
        }
        throw new CoreException(ErrorType.ACCESS_DENIED);
    }

    private TempUser requireUser(TempUser user) {
        if (user == null || user.role() == null) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
        return user;
    }

    private boolean canAccess(TempUser user, Long ownerEmpId, Long clientId) {
        if (user.role() == Role.ADMIN) {
            return true;
        }
        if (user.role() == Role.SALES_REP) {
            return user.employeeId() != null && user.employeeId().equals(ownerEmpId);
        }
        if (user.role() == Role.CLIENT) {
            return user.clientId() != null && user.clientId().equals(clientId);
        }
        return false;
    }
}
