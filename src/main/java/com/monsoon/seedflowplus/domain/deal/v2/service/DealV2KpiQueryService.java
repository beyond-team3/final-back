package com.monsoon.seedflowplus.domain.deal.v2.service;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.DocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.entity.QDocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.DocumentSummaryRepository;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealSearchCondition;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealKpiDto;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealV2KpiQueryService {

    private static final int KPI_SCAN_PAGE_SIZE = 10_000;

    private final SalesDealRepository salesDealRepository;
    private final DocumentSummaryRepository documentSummaryRepository;
    private final QuotationRepository quotationRepository;
    private final ContractRepository contractRepository;

    public DealKpiDto getKpis(
            SalesDealSearchCondition condition,
            CustomUserDetails userDetails
    ) {
        validateUser(userDetails);

        List<SalesDeal> deals = salesDealRepository.searchDeals(
                        enforceScope(condition, userDetails),
                        PageRequest.of(0, KPI_SCAN_PAGE_SIZE, Sort.by(Sort.Order.desc("lastActivityAt")))
                )
                .getContent();

        if (deals.isEmpty()) {
            return DealKpiDto.builder()
                    .dealCount(0)
                    .openDealCount(0)
                    .closedDealCount(0)
                    .successfulDealCount(0)
                    .successRate(BigDecimal.ZERO)
                    .averageLeadTimeDays(BigDecimal.ZERO)
                    .quotationToContractConversionRate(BigDecimal.ZERO)
                    .rewriteRate(BigDecimal.ZERO)
                    .build();
        }

        List<Long> dealIds = deals.stream().map(SalesDeal::getId).toList();
        Map<Long, List<DocumentSummary>> docsByDealId = loadDocumentsByDealId(dealIds);

        long dealCount = deals.size();
        long openDealCount = deals.stream().filter(deal -> deal.getClosedAt() == null).count();
        long closedDealCount = dealCount - openDealCount;

        Set<Long> successfulDealIds = docsByDealId.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(doc ->
                        doc.getDocType() == DealType.PAY && "COMPLETED".equals(doc.getStatus())))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        long successfulDealCount = successfulDealIds.size();
        BigDecimal successRate = ratio(successfulDealCount, dealCount);

        BigDecimal averageLeadTimeDays = averageLeadTimeDays(
                deals.stream().collect(Collectors.toMap(SalesDeal::getId, Function.identity())),
                docsByDealId,
                successfulDealIds
        );

        long quotationDealCount = docsByDealId.values().stream()
                .filter(docs -> docs.stream().anyMatch(doc -> doc.getDocType() == DealType.QUO))
                .count();
        long convertedDealCount = docsByDealId.values().stream()
                .filter(docs -> docs.stream().anyMatch(doc -> doc.getDocType() == DealType.QUO))
                .filter(docs -> docs.stream().anyMatch(doc -> doc.getDocType() == DealType.CNT))
                .count();
        BigDecimal quotationToContractConversionRate = ratio(convertedDealCount, quotationDealCount);

        Set<Long> revisedDealIds = quotationRepository.findAll().stream()
                .filter(quotation -> quotation.getDeal() != null && dealIds.contains(quotation.getDeal().getId()))
                .filter(quotation -> quotation.getSourceDocumentId() != null)
                .map(quotation -> quotation.getDeal().getId())
                .collect(Collectors.toSet());
        revisedDealIds.addAll(
                contractRepository.findAll().stream()
                        .filter(contract -> contract.getDeal() != null && dealIds.contains(contract.getDeal().getId()))
                        .filter(contract -> contract.getSourceDocumentId() != null)
                        .map(contract -> contract.getDeal().getId())
                        .toList()
        );
        BigDecimal rewriteRate = ratio(revisedDealIds.size(), dealCount);

        return DealKpiDto.builder()
                .dealCount(dealCount)
                .openDealCount(openDealCount)
                .closedDealCount(closedDealCount)
                .successfulDealCount(successfulDealCount)
                .successRate(successRate)
                .averageLeadTimeDays(averageLeadTimeDays)
                .quotationToContractConversionRate(quotationToContractConversionRate)
                .rewriteRate(rewriteRate)
                .build();
    }

    private Map<Long, List<DocumentSummary>> loadDocumentsByDealId(List<Long> dealIds) {
        QDocumentSummary documentSummary = QDocumentSummary.documentSummary;
        return StreamSupport.stream(
                        documentSummaryRepository.findAll(documentSummary.dealId.in(dealIds)).spliterator(),
                        false
                )
                .collect(Collectors.groupingBy(DocumentSummary::getDealId));
    }

    private BigDecimal averageLeadTimeDays(
            Map<Long, SalesDeal> dealsById,
            Map<Long, List<DocumentSummary>> docsByDealId,
            Set<Long> successfulDealIds
    ) {
        List<BigDecimal> leadTimes = successfulDealIds.stream()
                .map(dealsById::get)
                .filter(java.util.Objects::nonNull)
                .map(deal -> {
                    List<DocumentSummary> docs = docsByDealId.getOrDefault(deal.getId(), List.of());
                    LocalDateTime paidAt = docs.stream()
                            .filter(doc -> doc.getDocType() == DealType.PAY && "COMPLETED".equals(doc.getStatus()))
                            .map(DocumentSummary::getCreatedAt)
                            .filter(java.util.Objects::nonNull)
                            .min(LocalDateTime::compareTo)
                            .orElse(null);
                    if (paidAt == null || deal.getCreatedAt() == null) {
                        return null;
                    }
                    long days = Math.max(0, Duration.between(deal.getCreatedAt(), paidAt).toDays());
                    return BigDecimal.valueOf(days);
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        if (leadTimes.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = leadTimes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(leadTimes.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private SalesDealSearchCondition enforceScope(SalesDealSearchCondition condition, CustomUserDetails userDetails) {
        SalesDealSearchCondition base = condition == null
                ? SalesDealSearchCondition.builder().build()
                : condition;

        if (userDetails.getRole() == Role.ADMIN) {
            return base;
        }
        if (userDetails.getRole() == Role.SALES_REP) {
            return base.toBuilder().ownerEmpId(userDetails.getEmployeeId()).build();
        }
        if (userDetails.getRole() == Role.CLIENT) {
            return base.toBuilder().clientId(userDetails.getClientId()).build();
        }
        throw new AccessDeniedException("허용되지 않은 역할입니다: " + userDetails.getRole());
    }

    private void validateUser(CustomUserDetails userDetails) {
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
}
