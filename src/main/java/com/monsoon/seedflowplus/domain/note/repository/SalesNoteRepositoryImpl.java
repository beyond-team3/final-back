package com.monsoon.seedflowplus.domain.note.repository;

import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import com.monsoon.seedflowplus.domain.note.entity.QSalesNote;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

public class SalesNoteRepositoryImpl implements SalesNoteRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public SalesNoteRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<SalesNote> searchNotes(
            Long clientId,
            String contractId,
            String keyword,
            LocalDate from,
            LocalDate to,
            String sort
    ) {

        QSalesNote n = QSalesNote.salesNote;
        BooleanBuilder condition = new BooleanBuilder();

        // client 필터
        if (clientId != null) {
            condition.and(n.clientId.eq(clientId));
        }

        // contract 필터
        if (contractId != null && !contractId.isBlank()) {
            condition.and(n.contractId.eq(contractId));
        }

        // 키워드 검색
        if (keyword != null && !keyword.isBlank()) {
            condition.and(n.content.contains(keyword));
        }

        // 기간 필터
        if (from != null) {
            condition.and(n.activityDate.goe(from));
        }

        if (to != null) {
            condition.and(n.activityDate.loe(to));
        }

        // 정렬 처리
        OrderSpecifier<?> orderBy = isAsc(sort)
                ? n.activityDate.asc()
                : n.activityDate.desc();

        return queryFactory
                .selectFrom(n)
                .where(condition)
                .orderBy(orderBy)
                .fetch();
    }

    private boolean isAsc(String sort) {
        return "ASC".equalsIgnoreCase(sort);
    }
}