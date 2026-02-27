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

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from 날짜는 to 날짜보다 이후일 수 없습니다.");
        }

        QSalesNote n = QSalesNote.salesNote;
        BooleanBuilder condition = new BooleanBuilder();

        if (clientId != null) {
            condition.and(n.clientId.eq(clientId));
        }

        if (contractId != null && !contractId.isBlank()) {
            condition.and(n.contractId.eq(contractId));
        }

        if (keyword != null && !keyword.isBlank()) {
            condition.and(n.content.contains(keyword));
        }

        if (from != null && to != null) {
            condition.and(n.activityDate.between(from, to));
        } else if (from != null) {
            condition.and(n.activityDate.goe(from));
        } else if (to != null) {
            condition.and(n.activityDate.loe(to));
        }

        OrderSpecifier<?> orderBy =
                "ASC".equalsIgnoreCase(sort)
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