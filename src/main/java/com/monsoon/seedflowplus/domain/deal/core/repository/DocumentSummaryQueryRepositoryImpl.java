package com.monsoon.seedflowplus.domain.deal.core.repository;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.DocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.entity.QDocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
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
                .where(buildPredicates(userDetails, condition))
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(documentSummary.count())
                .from(documentSummary)
                .leftJoin(deal).on(documentSummary.dealId.eq(deal.id))
                .where(buildPredicates(userDetails, condition))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private Predicate[] buildPredicates(
            CustomUserDetails userDetails,
            DocumentSummarySearchCondition condition
    ) {
        return new Predicate[]{
                excludePayType(),
                excludeDeletedStatus(),
                clientVisibleAfterAdminApproval(userDetails),
                roleScope(userDetails),
                ownerEmpIdEq(condition.ownerEmpId()),
                clientIdEq(condition.clientId()),
                docTypeEq(condition.docType()),
                statusEq(condition.status()),
                keywordContains(condition.keyword())
        };
    }

    private BooleanExpression roleScope(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getRole() == null) {
            throw new AccessDeniedException("사용자 권한 정보가 없습니다.");
        }

        Role role = userDetails.getRole();
        if (role == Role.ADMIN) {
            return null;
        }
        if (role == Role.SALES_REP) {
            Long employeeId = userDetails.getEmployeeId();
            if (employeeId == null) {
                throw new IllegalArgumentException("SALES_REP 사용자에 employeeId가 없습니다.");
            }
            return deal.ownerEmp.id.eq(employeeId);
        }
        if (role == Role.CLIENT) {
            Long clientId = userDetails.getClientId();
            if (clientId == null) {
                throw new IllegalArgumentException("CLIENT 사용자에 clientId가 없습니다.");
            }
            return documentSummary.clientId.eq(clientId);
        }
        throw new AccessDeniedException("허용되지 않은 역할입니다: " + role);
    }

    private BooleanExpression ownerEmpIdEq(Long ownerEmpId) {
        return ownerEmpId == null ? null : deal.ownerEmp.id.eq(ownerEmpId);
    }

    private BooleanExpression excludePayType() {
        return documentSummary.docType.ne(DealType.PAY);
    }

    private BooleanExpression excludeDeletedStatus() {
        return documentSummary.status.ne("DELETED");
    }

    private BooleanExpression clientVisibleAfterAdminApproval(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getRole() != Role.CLIENT) {
            return null;
        }
        return documentSummary.docType.in(DealType.QUO, DealType.CNT)
                .and(documentSummary.status.in("WAITING_ADMIN", "REJECTED_ADMIN"))
                .not();
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

    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (pageable != null) {
            for (Sort.Order sortOrder : pageable.getSort()) {
                if ("createdAt".equals(sortOrder.getProperty())) {
                    orderSpecifiers.add(new OrderSpecifier<>(
                            sortOrder.isAscending() ? Order.ASC : Order.DESC,
                            documentSummary.createdAt
                    ));
                }
            }
        }

        if (orderSpecifiers.isEmpty()) {
            orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, documentSummary.createdAt));
        }

        return orderSpecifiers.toArray(OrderSpecifier[]::new);
    }
}
