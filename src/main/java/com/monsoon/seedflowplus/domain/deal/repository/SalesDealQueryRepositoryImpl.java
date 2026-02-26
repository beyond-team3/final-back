package com.monsoon.seedflowplus.domain.deal.repository;

import com.monsoon.seedflowplus.domain.account.entity.QClient;
import com.monsoon.seedflowplus.domain.account.entity.QEmployee;
import com.monsoon.seedflowplus.domain.deal.entity.DealStage;
import com.monsoon.seedflowplus.domain.deal.entity.DealType;
import com.monsoon.seedflowplus.domain.deal.entity.QSalesDeal;
import com.monsoon.seedflowplus.domain.deal.entity.SalesDeal;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
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
