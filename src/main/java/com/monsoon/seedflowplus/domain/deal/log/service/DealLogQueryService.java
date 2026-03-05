package com.monsoon.seedflowplus.domain.deal.log.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogDetailDto;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogSummaryDto;
import com.monsoon.seedflowplus.domain.deal.log.entity.DealLogDetail;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import com.monsoon.seedflowplus.domain.deal.log.entity.SalesDealLog;
import com.monsoon.seedflowplus.domain.deal.log.repository.DealLogDetailRepository;
import com.monsoon.seedflowplus.domain.deal.log.repository.SalesDealLogRepository;
import java.util.List;
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
    private static final int DEFAULT_RECENT_LIMIT = 20;

    private final SalesDealLogRepository salesDealLogRepository;
    private final DealLogDetailRepository dealLogDetailRepository;

    public Page<DealLogSummaryDto> getTimelineByDeal(Long dealId, Pageable pageable, CustomUserDetails user) {
        Pageable resolvedPageable = resolveTimelinePageable(pageable);
        CustomUserDetails requiredUser = requireUser(user);

        return findByDealWithScope(dealId, resolvedPageable, requiredUser)
                .map(this::toSummaryDto);
    }

    public Page<DealLogSummaryDto> getTimelineByClient(Long clientId, Pageable pageable, CustomUserDetails user) {
        Pageable resolvedPageable = resolveTimelinePageable(pageable);
        CustomUserDetails requiredUser = requireUser(user);

        return findByClientWithScope(clientId, resolvedPageable, requiredUser)
                .map(this::toSummaryDto);
    }

    public Page<DealLogSummaryDto> getTimelineByDocument(DealType docType, Long refId, Pageable pageable, CustomUserDetails user) {
        Pageable resolvedPageable = resolveTimelinePageable(pageable);
        CustomUserDetails requiredUser = requireUser(user);

        return findByDocumentWithScope(docType, refId, resolvedPageable, requiredUser)
                .map(this::toSummaryDto);
    }

    public DealLogDetailDto getLogDetail(Long dealLogId, CustomUserDetails user) {
        CustomUserDetails requiredUser = requireUser(user);
        SalesDealLog log = salesDealLogRepository.findById(dealLogId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.DEAL_LOG_DETAIL_NOT_FOUND,
                        "DealLog을 찾을 수 없습니다. dealLogId=" + dealLogId
                ));
        DealLogDetail detail = dealLogDetailRepository.findByDealLogId(dealLogId).orElse(null);

        Long clientId = log.getClient() != null
                ? log.getClient().getId()
                : (log.getDeal() != null && log.getDeal().getClient() != null ? log.getDeal().getClient().getId() : null);
        Long ownerEmpId = (log.getDeal() != null && log.getDeal().getOwnerEmp() != null)
                ? log.getDeal().getOwnerEmp().getId()
                : null;
        if (!canAccess(requiredUser, ownerEmpId, clientId)) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        return toDetailDto(log, detail);
    }

    public List<DealLogSummaryDto> getRecentDocumentLogs(Long dealId, DealType docType, Long refId) {
        return getRecentDocumentLogs(dealId, docType, refId, DEFAULT_RECENT_LIMIT);
    }

    public List<DealLogSummaryDto> getRecentDocumentLogs(Long dealId, DealType docType, Long refId, int limit) {
        if (dealId == null || docType == null || refId == null) {
            return List.of();
        }
        return getRecentDocumentLogsStrict(dealId, docType, refId, limit);
    }

    public List<DealLogSummaryDto> getRecentDocumentLogsStrict(Long dealId, DealType docType, Long refId, int limit) {
        requireRecentLogParam("dealId", dealId);
        requireRecentLogParam("docType", docType);
        requireRecentLogParam("refId", refId);
        int resolvedLimit = limit > 0 ? limit : DEFAULT_RECENT_LIMIT;
        Pageable pageable = PageRequest.of(0, resolvedLimit);
        return salesDealLogRepository.findRecentByDealIdAndDocTypeAndRefId(dealId, docType, refId, pageable)
                .stream()
                .map(this::toSummaryDto)
                .toList();
    }

    private void requireRecentLogParam(String name, Object value) {
        if (value == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE, "getRecentDocumentLogs: " + name + " must not be null");
        }
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

    private DealLogDetailDto toDetailDto(SalesDealLog log, DealLogDetail detail) {
        return DealLogDetailDto.builder()
                .dealLogId(log.getId())
                .targetCode(log.getTargetCode())
                .reason(detail != null ? detail.getReason() : null)
                .diffJson(detail != null ? detail.getDiffJson() : null)
                .createdAt(detail != null ? detail.getCreatedAt() : null)
                .build();
    }

    private Page<SalesDealLog> findByDealWithScope(Long dealId, Pageable pageable, CustomUserDetails user) {
        if (user.getRole() == Role.ADMIN) {
            return salesDealLogRepository.findByDealId(dealId, pageable);
        }
        if (user.getRole() == Role.SALES_REP) {
            if (user.getEmployeeId() == null) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return salesDealLogRepository.findByDealIdAndOwnerEmpId(dealId, user.getEmployeeId(), pageable);
        }
        if (user.getRole() == Role.CLIENT) {
            if (user.getClientId() == null) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return salesDealLogRepository.findByDealIdAndClientId(dealId, user.getClientId(), pageable);
        }
        throw new CoreException(ErrorType.ACCESS_DENIED);
    }

    private Page<SalesDealLog> findByClientWithScope(Long clientId, Pageable pageable, CustomUserDetails user) {
        if (user.getRole() == Role.ADMIN) {
            return salesDealLogRepository.findByClientId(clientId, pageable);
        }
        if (user.getRole() == Role.SALES_REP) {
            if (user.getEmployeeId() == null) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return salesDealLogRepository.findByClientIdAndOwnerEmpId(clientId, user.getEmployeeId(), pageable);
        }
        if (user.getRole() == Role.CLIENT) {
            if (user.getClientId() == null || !user.getClientId().equals(clientId)) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return salesDealLogRepository.findByClientId(clientId, pageable);
        }
        throw new CoreException(ErrorType.ACCESS_DENIED);
    }

    private Page<SalesDealLog> findByDocumentWithScope(DealType docType, Long refId, Pageable pageable, CustomUserDetails user) {
        if (user.getRole() == Role.ADMIN) {
            return salesDealLogRepository.findByDocTypeAndRefId(docType, refId, pageable);
        }
        if (user.getRole() == Role.SALES_REP) {
            if (user.getEmployeeId() == null) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return salesDealLogRepository.findByDocTypeAndRefIdAndOwnerEmpId(docType, refId, user.getEmployeeId(), pageable);
        }
        if (user.getRole() == Role.CLIENT) {
            if (user.getClientId() == null) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return salesDealLogRepository.findByDocTypeAndRefIdAndClientId(docType, refId, user.getClientId(), pageable);
        }
        throw new CoreException(ErrorType.ACCESS_DENIED);
    }

    private CustomUserDetails requireUser(CustomUserDetails user) {
        if (user == null || user.getRole() == null) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
        return user;
    }

    private boolean canAccess(CustomUserDetails user, Long ownerEmpId, Long clientId) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (user.getRole() == Role.SALES_REP) {
            return user.getEmployeeId() != null && user.getEmployeeId().equals(ownerEmpId);
        }
        if (user.getRole() == Role.CLIENT) {
            return user.getClientId() != null && user.getClientId().equals(clientId);
        }
        return false;
    }
}
