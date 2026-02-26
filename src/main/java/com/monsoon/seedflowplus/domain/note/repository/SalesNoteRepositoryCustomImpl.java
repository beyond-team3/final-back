package com.monsoon.seedflowplus.domain.note.repository;

import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.util.List;

public class SalesNoteRepositoryCustomImpl implements SalesNoteRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<SalesNote> searchNotes(
            Long clientId,
            String contractId,
            String keyword,
            LocalDate from,
            LocalDate to,
            String sort
    ) {

        String jpql = "SELECT n FROM SalesNote n WHERE 1=1 ";

        if (clientId != null) {
            jpql += " AND n.clientId = :clientId";
        }

        if (keyword != null && !keyword.isBlank()) {
            jpql += " AND n.content LIKE :keyword";
        }

        if (from != null) {
            jpql += " AND n.activityDate >= :from";
        }

        if (to != null) {
            jpql += " AND n.activityDate <= :to";
        }

        jpql += " ORDER BY n.activityDate DESC";

        TypedQuery<SalesNote> query = em.createQuery(jpql, SalesNote.class);

        if (clientId != null) {
            query.setParameter("clientId", clientId);
        }

        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("keyword", "%" + keyword + "%");
        }

        if (from != null) {
            query.setParameter("from", from);
        }

        if (to != null) {
            query.setParameter("to", to);
        }

        return query.getResultList();
    }
}