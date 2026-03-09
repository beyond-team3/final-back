package com.monsoon.seedflowplus.domain.deal.core.repository;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.DocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.entity.QDocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class DocumentSummaryQueryRepositoryImpl implements DocumentSummaryQueryRepository {

    private static final QDocumentSummary documentSummary = QDocumentSummary.documentSummary;
    private static final QSalesDeal deal = QSalesDeal.salesDeal;

    private final JPAQueryFactory queryFactory;

    public DocumentSummaryQueryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<DocumentSummary> searchDocuments(
            DocumentSummarySearchCondition cond,
            Pageable pageable,
            CustomUserDetails userDetails
    ) {
        DocumentSummarySearchCondition condition = cond == null
                ? DocumentSummarySearchCondition.builder().build()
                : cond;

        List<DocumentSummary> content = queryFactory
                .selectFrom(documentSummary)
                .leftJoin(deal).on(documentSummary.dealId.eq(deal.id))
                .where(
                        roleScope(userDetails),
                        ownerEmpIdEq(condition.ownerEmpId()),
                        clientIdEq(condition.clientId()),
                        docTypeEq(condition.docType()),
                        statusEq(condition.status()),
                        keywordContains(condition.keyword())
                )
                .orderBy(createdAtDesc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(documentSummary.count())
                .from(documentSummary)
                .leftJoin(deal).on(documentSummary.dealId.eq(deal.id))
                .where(
                        roleScope(userDetails),
                        ownerEmpIdEq(condition.ownerEmpId()),
                        clientIdEq(condition.clientId()),
                        docTypeEq(condition.docType()),
                        statusEq(condition.status()),
                        keywordContains(condition.keyword())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private BooleanExpression roleScope(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getRole() == null) {
            return null;
        }

        Role role = userDetails.getRole();
        if (role == Role.ADMIN) {
            return null;
        }
        if (role == Role.SALES_REP) {
            return deal.ownerEmp.id.eq(userDetails.getEmployeeId());
        }
        if (role == Role.CLIENT) {
            return documentSummary.clientId.eq(userDetails.getClientId());
        }
        throw new AccessDeniedException("허용되지 않은 역할입니다: " + role);
    }

    private BooleanExpression ownerEmpIdEq(Long ownerEmpId) {
        return ownerEmpId == null ? null : deal.ownerEmp.id.eq(ownerEmpId);
    }

    private BooleanExpression clientIdEq(Long clientId) {
        return clientId == null ? null : documentSummary.clientId.eq(clientId);
    }

    private BooleanExpression docTypeEq(DealType docType) {
        return docType == null ? null : documentSummary.docType.eq(docType);
    }

    private BooleanExpression statusEq(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return documentSummary.status.eq(status.trim());
    }

    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return documentSummary.docCode.containsIgnoreCase(keyword.trim());
    }

    private OrderSpecifier<?> createdAtDesc() {
        return new OrderSpecifier<>(Order.DESC, documentSummary.createdAt);
    }
}
