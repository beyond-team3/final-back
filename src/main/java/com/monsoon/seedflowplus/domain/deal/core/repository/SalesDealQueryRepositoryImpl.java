package com.monsoon.seedflowplus.domain.deal.core.repository;

import com.monsoon.seedflowplus.domain.account.entity.QClient;
import com.monsoon.seedflowplus.domain.account.entity.QEmployee;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.QDocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.contract.entity.QContractHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QQuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.request.entity.QQuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class SalesDealQueryRepositoryImpl implements SalesDealQueryRepository {

    private static final QSalesDeal deal = QSalesDeal.salesDeal;
    private static final QClient client = QClient.client;
    private static final QEmployee ownerEmp = QEmployee.employee;
    private static final QDocumentSummary documentSummary = QDocumentSummary.documentSummary;
    private static final QQuotationRequestHeader quotationRequest = QQuotationRequestHeader.quotationRequestHeader;
    private static final QQuotationHeader quotation = QQuotationHeader.quotationHeader;
    private static final QContractHeader contract = QContractHeader.contractHeader;

    private final JPAQueryFactory queryFactory;

    public SalesDealQueryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<SalesDeal> searchDeals(SalesDealSearchCondition cond, Pageable pageable) {
        SalesDealSearchCondition condition = cond == null ? SalesDealSearchCondition.builder().build() : cond;

        List<SalesDeal> content = queryFactory
                .selectFrom(deal)
                .leftJoin(deal.client, client)
                .leftJoin(deal.ownerEmp, ownerEmp)
                .where(
                        visibleInHistory(),
                        clientPostAdminApprovalVisible(condition.getClientPostAdminApprovalOnly()),
                        ownerEmpIdEq(condition.getOwnerEmpId()),
                        clientIdEq(condition.getClientId()),
                        currentStageEq(condition.getCurrentStage()),
                        latestDocTypeEq(condition.getLatestDocType()),
                        closedEq(condition.getIsClosed()),
                        keywordContains(condition.getKeyword()),
                        lastActivityAtBetween(condition.getFromAt(), condition.getToAt())
                )
                .orderBy(orderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(deal.count())
                .from(deal)
                .leftJoin(deal.client, client)
                .leftJoin(deal.ownerEmp, ownerEmp)
                .where(
                        visibleInHistory(),
                        clientPostAdminApprovalVisible(condition.getClientPostAdminApprovalOnly()),
                        ownerEmpIdEq(condition.getOwnerEmpId()),
                        clientIdEq(condition.getClientId()),
                        currentStageEq(condition.getCurrentStage()),
                        latestDocTypeEq(condition.getLatestDocType()),
                        closedEq(condition.getIsClosed()),
                        keywordContains(condition.getKeyword()),
                        lastActivityAtBetween(condition.getFromAt(), condition.getToAt())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private BooleanExpression ownerEmpIdEq(Long ownerEmpId) {
        return ownerEmpId == null ? null : deal.ownerEmp.id.eq(ownerEmpId);
    }

    private BooleanExpression clientPostAdminApprovalVisible(Boolean clientPostAdminApprovalOnly) {
        if (!Boolean.TRUE.equals(clientPostAdminApprovalOnly)) {
            return null;
        }
        return deal.latestDocType.in(DealType.QUO, DealType.CNT)
                .and(deal.currentStage.in(DealStage.PENDING_ADMIN, DealStage.REJECTED_ADMIN))
                .not();
    }

    private BooleanExpression visibleInHistory() {
        return bootstrapPlaceholder().not()
                .and(deletedPreDocumentOnlyDeal().not());
    }

    private BooleanExpression bootstrapPlaceholder() {
        return deal.currentStage.eq(DealStage.CREATED)
                .and(deal.currentStatus.eq("PENDING"))
                .and(deal.latestDocType.eq(DealType.RFQ))
                .and(deal.latestRefId.eq(0L));
    }

    private BooleanExpression deletedPreDocumentOnlyDeal() {
        return deletedPreDocumentExists()
                .and(meaningfulDocumentExists().not());
    }

    private BooleanExpression deletedPreDocumentExists() {
        return deletedRequestExists()
                .or(deletedQuotationExists())
                .or(deletedContractExists());
    }

    private BooleanExpression meaningfulDocumentExists() {
        return JPAExpressions.selectOne()
                .from(documentSummary)
                .where(
                        documentSummary.dealId.eq(deal.id),
                        deletedPreDocumentStatus().not()
                )
                .exists();
    }

    private BooleanExpression deletedPreDocumentStatus() {
        return documentSummary.docType.in(DealType.RFQ, DealType.QUO, DealType.CNT)
                .and(documentSummary.status.eq("DELETED"));
    }

    private BooleanExpression deletedRequestExists() {
        return JPAExpressions.selectOne()
                .from(quotationRequest)
                .where(
                        quotationRequest.deal.id.eq(deal.id),
                        quotationRequest.status.eq(QuotationRequestStatus.DELETED)
                )
                .exists();
    }

    private BooleanExpression deletedQuotationExists() {
        return JPAExpressions.selectOne()
                .from(quotation)
                .where(
                        quotation.deal.id.eq(deal.id),
                        quotation.status.eq(QuotationStatus.DELETED)
                )
                .exists();
    }

    private BooleanExpression deletedContractExists() {
        return JPAExpressions.selectOne()
                .from(contract)
                .where(
                        contract.deal.id.eq(deal.id),
                        contract.status.eq(ContractStatus.DELETED)
                )
                .exists();
    }

    private BooleanExpression clientIdEq(Long clientId) {
        return clientId == null ? null : deal.client.id.eq(clientId);
    }

    private BooleanExpression currentStageEq(DealStage currentStage) {
        return currentStage == null ? null : deal.currentStage.eq(currentStage);
    }

    private BooleanExpression latestDocTypeEq(DealType latestDocType) {
        return latestDocType == null ? null : deal.latestDocType.eq(latestDocType);
    }

    private BooleanExpression closedEq(Boolean isClosed) {
        if (isClosed == null) {
            return null;
        }
        return isClosed ? deal.closedAt.isNotNull() : deal.closedAt.isNull();
    }

    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }

        return client.clientName.containsIgnoreCase(keyword.trim())
                .or(deal.latestTargetCode.containsIgnoreCase(keyword.trim()))
                .or(deal.summaryMemo.containsIgnoreCase(keyword.trim()));
    }

    private BooleanExpression lastActivityAtBetween(LocalDateTime fromAt, LocalDateTime toAt) {
        if (fromAt == null && toAt == null) {
            return null;
        }
        if (fromAt != null && toAt != null) {
            return deal.lastActivityAt.between(fromAt, toAt);
        }
        if (fromAt != null) {
            return deal.lastActivityAt.goe(fromAt);
        }
        return deal.lastActivityAt.loe(toAt);
    }

    private OrderSpecifier<?>[] orderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {
            if ("lastActivityAt".equals(order.getProperty())) {
                orderSpecifiers.add(new OrderSpecifier<>(
                        order.isAscending() ? Order.ASC : Order.DESC,
                        deal.lastActivityAt
                ));
            } else if ("closedAt".equals(order.getProperty())) {
                orderSpecifiers.add(new OrderSpecifier<>(
                        order.isAscending() ? Order.ASC : Order.DESC,
                        deal.closedAt
                ));
            }
        }

        if (orderSpecifiers.isEmpty()) {
            orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, deal.lastActivityAt));
        }

        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }
}
