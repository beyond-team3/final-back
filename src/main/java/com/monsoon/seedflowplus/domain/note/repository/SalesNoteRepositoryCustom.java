package com.monsoon.seedflowplus.domain.note.repository;

import com.monsoon.seedflowplus.domain.note.entity.SalesNote;

import java.time.LocalDate;
import java.util.List;

public interface SalesNoteRepositoryCustom {

    List<SalesNote> searchNotes(
            Long clientId,
            String contractId,
            String keyword,
            LocalDate from,
            LocalDate to,
            String sort
    );
}