package com.monsoon.seedflowplus.domain.statistics.billing.repository;

import static com.monsoon.seedflowplus.domain.billing.invoice.entity.QInvoice.invoice;
import static com.monsoon.seedflowplus.domain.billing.invoice.entity.QInvoiceStatement.invoiceStatement;
import static com.monsoon.seedflowplus.domain.billing.statement.entity.QStatement.statement;
import static com.monsoon.seedflowplus.domain.sales.contract.entity.QContractDetail.contractDetail;
import static com.monsoon.seedflowplus.domain.sales.order.entity.QOrderDetail.orderDetail;

import com.monsoon.seedflowplus.domain.billing.invoice.entity.QInvoiceStatement;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import com.monsoon.seedflowplus.domain.billing.statement.entity.StatementStatus;
import com.monsoon.seedflowplus.domain.billing.statement.entity.QStatement;
import com.monsoon.seedflowplus.domain.sales.contract.entity.QContractDetail;
import com.monsoon.seedflowplus.domain.sales.order.entity.QOrderDetail;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.request.BillingRevenueStatisticsFilter;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.CategoryBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyCategoryBilledRevenueDto;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class BillingRevenueStatisticsRepository {

    private final JPAQueryFactory queryFactory;

    public List<MonthlyBilledRevenueDto> findMonthlyRevenue(BillingRevenueStatisticsFilter filter) {
        StringExpression monthExpr = monthExpression();
        NumberExpression<BigDecimal> invoiceAmountExpr = invoice.totalAmount.coalesce(BigDecimal.ZERO);

        if (!StringUtils.hasText(filter.getCategory())) {
            NumberExpression<BigDecimal> sumExpr = invoiceAmountExpr.sum().coalesce(BigDecimal.ZERO);

            List<Tuple> rows = queryFactory
                    .select(monthExpr, sumExpr)
                    .from(invoice)
                    .where(
                            invoice.status.in(InvoiceStatus.PUBLISHED, InvoiceStatus.PAID),
                            invoice.invoiceDate.between(filter.getFromDate(), filter.getToDate()),
                            hasIncludedIssuedStatement(null)
                    )
                    .groupBy(monthExpr)
                    .orderBy(monthExpr.asc())
                    .fetch();

            return rows.stream()
                    .map(row -> new MonthlyBilledRevenueDto(
                            row.get(monthExpr),
                            Objects.requireNonNullElse(row.get(sumExpr), BigDecimal.ZERO)
                    ))
                    .toList();
        }

        NumberExpression<BigDecimal> categoryAmountExpr = billedLineAmountExpression();
        NumberExpression<BigDecimal> groupedAmountExpr = categoryAmountExpr.sum().coalesce(BigDecimal.ZERO);

        List<Tuple> rows = queryFactory
                .select(monthExpr, invoice.id, groupedAmountExpr)
                .from(invoice)
                .join(invoiceStatement).on(invoiceStatement.invoice.id.eq(invoice.id))
                .join(statement).on(statement.id.eq(invoiceStatement.statement.id))
                .join(orderDetail).on(orderDetail.orderHeader.id.eq(statement.orderHeader.id))
                .join(contractDetail).on(contractDetail.id.eq(orderDetail.contractDetail.id))
                .where(
                        invoice.status.in(InvoiceStatus.PUBLISHED, InvoiceStatus.PAID),
                        invoice.invoiceDate.between(filter.getFromDate(), filter.getToDate()),
                        invoiceStatement.included.isTrue(),
                        statement.status.eq(StatementStatus.ISSUED),
                        categoryEq(filter.getCategory())
                )
                .groupBy(monthExpr, invoice.id)
                .fetch();

        Map<String, BigDecimal> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        row -> row.get(monthExpr),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                row -> Objects.requireNonNullElse(row.get(groupedAmountExpr), BigDecimal.ZERO),
                                BigDecimal::add
                        )
                ));

        return grouped.entrySet().stream()
                .map(entry -> new MonthlyBilledRevenueDto(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(MonthlyBilledRevenueDto::getMonth))
                .toList();
    }

    public List<CategoryBilledRevenueDto> findCategoryRevenue(BillingRevenueStatisticsFilter filter) {
        StringExpression categoryExpr = contractDetail.productCategory;
        NumberExpression<BigDecimal> lineAmountExpr = billedLineAmountExpression();
        NumberExpression<BigDecimal> groupedAmountExpr = lineAmountExpr.sum().coalesce(BigDecimal.ZERO);

        List<Tuple> rows = queryFactory
                .select(categoryExpr, invoice.id, groupedAmountExpr)
                .from(invoice)
                .join(invoiceStatement).on(invoiceStatement.invoice.id.eq(invoice.id))
                .join(statement).on(statement.id.eq(invoiceStatement.statement.id))
                .join(orderDetail).on(orderDetail.orderHeader.id.eq(statement.orderHeader.id))
                .join(contractDetail).on(contractDetail.id.eq(orderDetail.contractDetail.id))
                .where(
                        invoice.status.in(InvoiceStatus.PUBLISHED, InvoiceStatus.PAID),
                        invoice.invoiceDate.between(filter.getFromDate(), filter.getToDate()),
                        invoiceStatement.included.isTrue(),
                        statement.status.eq(StatementStatus.ISSUED),
                        categoryEq(filter.getCategory())
                )
                // invoice+category 단위로 1회만 집계되도록 중복 제거
                .groupBy(categoryExpr, invoice.id)
                .fetch();

        Map<String, BigDecimal> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        row -> row.get(categoryExpr),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                row -> Objects.requireNonNullElse(row.get(groupedAmountExpr), BigDecimal.ZERO),
                                BigDecimal::add
                        )
                ));

        return grouped.entrySet().stream()
                .map(entry -> new CategoryBilledRevenueDto(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CategoryBilledRevenueDto::getBilledRevenue).reversed())
                .toList();
    }

    public List<MonthlyCategoryBilledRevenueDto> findMonthlyCategoryRevenue(BillingRevenueStatisticsFilter filter) {
        StringExpression monthExpr = monthExpression();
        StringExpression categoryExpr = contractDetail.productCategory;
        NumberExpression<BigDecimal> lineAmountExpr = billedLineAmountExpression();
        NumberExpression<BigDecimal> groupedAmountExpr = lineAmountExpr.sum().coalesce(BigDecimal.ZERO);

        List<Tuple> rows = queryFactory
                .select(monthExpr, categoryExpr, invoice.id, groupedAmountExpr)
                .from(invoice)
                .join(invoiceStatement).on(invoiceStatement.invoice.id.eq(invoice.id))
                .join(statement).on(statement.id.eq(invoiceStatement.statement.id))
                .join(orderDetail).on(orderDetail.orderHeader.id.eq(statement.orderHeader.id))
                .join(contractDetail).on(contractDetail.id.eq(orderDetail.contractDetail.id))
                .where(
                        invoice.status.in(InvoiceStatus.PUBLISHED, InvoiceStatus.PAID),
                        invoice.invoiceDate.between(filter.getFromDate(), filter.getToDate()),
                        invoiceStatement.included.isTrue(),
                        statement.status.eq(StatementStatus.ISSUED),
                        categoryEq(filter.getCategory())
                )
                // invoice+month+category 단위로 1회만 집계되도록 중복 제거
                .groupBy(monthExpr, categoryExpr, invoice.id)
                .fetch();

        Map<MonthCategoryKey, BigDecimal> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        row -> new MonthCategoryKey(row.get(monthExpr), row.get(categoryExpr)),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                row -> Objects.requireNonNullElse(row.get(groupedAmountExpr), BigDecimal.ZERO),
                                BigDecimal::add
                        )
                ));

        return grouped.entrySet().stream()
                .map(entry -> new MonthlyCategoryBilledRevenueDto(
                        entry.getKey().month(),
                        entry.getKey().category(),
                        entry.getValue()
                ))
                .sorted(Comparator.comparing(MonthlyCategoryBilledRevenueDto::getMonth))
                .toList();
    }

    private BooleanExpression categoryEq(String category) {
        return categoryEq(contractDetail.productCategory, category);
    }

    private BooleanExpression categoryEq(StringExpression categoryExpr, String category) {
        return StringUtils.hasText(category) ? categoryExpr.eq(category) : null;
    }

    private StringExpression monthExpression() {
        // MySQL 및 H2(MODE=MySQL) 환경 기준 월 키 포맷
        return Expressions.stringTemplate("DATE_FORMAT({0}, '%Y-%m')", invoice.invoiceDate);
    }

    private NumberExpression<BigDecimal> billedLineAmountExpression() {
        return Expressions.numberTemplate(
                BigDecimal.class,
                "coalesce({0}, 0) * coalesce({1}, 0)",
                contractDetail.unitPrice,
                orderDetail.quantity
        );
    }

    private BooleanExpression hasIncludedIssuedStatement(String category) {
        QInvoiceStatement isSub = new QInvoiceStatement("isSub");
        QStatement stSub = new QStatement("stSub");
        QOrderDetail odSub = new QOrderDetail("odSub");
        QContractDetail cdSub = new QContractDetail("cdSub");

        return JPAExpressions
                .selectOne()
                .from(isSub)
                .join(isSub.statement, stSub)
                .leftJoin(odSub).on(odSub.orderHeader.id.eq(stSub.orderHeader.id))
                .leftJoin(cdSub).on(cdSub.id.eq(odSub.contractDetail.id))
                .where(
                        isSub.invoice.id.eq(invoice.id),
                        isSub.included.isTrue(),
                        stSub.status.eq(StatementStatus.ISSUED),
                        categoryEq(cdSub.productCategory, category)
                )
                .exists();
    }

    private record MonthCategoryKey(String month, String category) {
    }
}
