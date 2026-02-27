package com.monsoon.seedflowplus.domain.product.repository;

import com.monsoon.seedflowplus.domain.product.dto.request.ProductSearchCondition;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.monsoon.seedflowplus.domain.product.entity.QCultivationTime.cultivationTime;
import static com.monsoon.seedflowplus.domain.product.entity.QProduct.product;

@RequiredArgsConstructor
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Product> searchByCondition(ProductSearchCondition condition) {
        return queryFactory
                .selectFrom(product)
                .leftJoin(cultivationTime).on(cultivationTime.product.id.eq(product.id))
                .where(
                        categoryEq(condition.getCategory()),
                        keywordContains(condition.getKeyword()),
                        sowingMonthBetween(condition.getSowingMonth()),
                        plantingMonthBetween(condition.getPlantingMonth()),
                        harvestingMonthBetween(condition.getHarvestingMonth()),
                        product.isDeleted.eq(false) // 논리 삭제 배제
                )
                .orderBy(product.id.desc())
                .fetch();
    }

    private BooleanExpression categoryEq(String category) {
        return StringUtils.hasText(category) ? product.productCategory.eq(ProductCategory.valueOf(category)) : null;
    }

    private BooleanExpression keywordContains(String keyword) {
        return StringUtils.hasText(keyword) ? product.productName.contains(keyword)
                .or(product.productDescription.contains(keyword)) : null;
    }

    // 대상 월이 Start와 End 사이에 존재하는지 판별 (월이 연도를 넘어가는 로직은 여기선 생략, 1~12월 단순 수치 비교)
    private BooleanExpression sowingMonthBetween(Integer month) {
        if (month == null)
            return null;
        return cultivationTime.sowingStart.loe(month)
                .and(cultivationTime.sowingEnd.goe(month));
    }

    private BooleanExpression plantingMonthBetween(Integer month) {
        if (month == null)
            return null;
        return cultivationTime.plantingStart.loe(month)
                .and(cultivationTime.plantingEnd.goe(month));
    }

    private BooleanExpression harvestingMonthBetween(Integer month) {
        if (month == null)
            return null;
        return cultivationTime.harvestingStart.loe(month)
                .and(cultivationTime.harvestingEnd.goe(month));
    }
}
