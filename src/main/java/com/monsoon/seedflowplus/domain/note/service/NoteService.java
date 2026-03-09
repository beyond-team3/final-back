package com.monsoon.seedflowplus.domain.note.service;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import com.monsoon.seedflowplus.domain.note.repository.SalesNoteRepository;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.note.dto.request.NoteRequestDto;
import com.monsoon.seedflowplus.domain.note.dto.NoteSearchCondition;
import com.monsoon.seedflowplus.infra.ai.AiClient;
import com.monsoon.seedflowplus.infra.ai.rag.SalesNoteRagService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

    private final SalesNoteRepository noteRepository;
    private final ClientRepository clientRepository;
    private final ContractRepository contractRepository;
    private final RagSeedService ragSeedService;
    private final AiClient aiClient;
    private final SalesNoteRagService ragService;

    /**
     * 1. 영업 활동 기록 목록 조회 및 검색
     */
    public List<SalesNote> searchNotes(NoteSearchCondition condition) {
        CustomUserDetails userDetails = getCurrentUserDetails();
        
        // 모든 사용자는 본인이 작성한 노트만 조회 가능 (authorId 필터 상시 적용)
        Long authorIdFilter = userDetails.getEmployeeId();

        return noteRepository.searchNotes(
                condition.getClientId(),
                condition.getContractId(),
                condition.getKeyword(),
                condition.getDateFrom(),
                condition.getDateTo(),
                condition.getSort(),
                authorIdFilter
        );
    }

    /**
     * 2. 영업 활동 기록 저장 및 AI 요약 분석 트리거
     */
    @Transactional
    public SalesNote createNote(NoteRequestDto dto) {
        CustomUserDetails userDetails = getCurrentUserDetails();
        Long currentEmployeeId = userDetails.getEmployeeId();
        Role role = userDetails.getRole();

        // 역할별 권한 검증
        if (role == Role.ADMIN) {
            if (!clientRepository.existsById(dto.getClientId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 거래처입니다.");
            }
        } else if (role == Role.SALES_REP) {
            Client client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 거래처입니다."));

            if (client.getManagerEmployee() == null || !client.getManagerEmployee().getId().equals(currentEmployeeId)) {
                throw new AccessDeniedException("본인이 담당하는 거래처의 노트만 작성할 수 있습니다.");
            }
        } else {
            throw new AccessDeniedException("노트 작성 권한이 없습니다.");
        }

        validateContractOwnership(dto.getContractId(), dto.getClientId());

        List<String> summary = aiClient.summarizeNote(dto.getContent());

        SalesNote note = SalesNote.builder()
                .clientId(dto.getClientId())
                .authorId(currentEmployeeId)
                .contractId(dto.getContractId())
                .activityDate(dto.getDate())
                .content(dto.getContent())
                .aiSummary(summary)
                .build();
        
        SalesNote savedNote = noteRepository.save(note);

        // [RAG] 벡터 DB 인덱싱 수행 (트랜잭션 커밋 후)
        triggerRagIndexingAfterCommit(savedNote);

        // 비동기 분석 트리거
        triggerBriefingUpdate(savedNote.getClientId());

        return savedNote;
    }

    /**
     * 3. 영업 활동 기록 수정 및 AI 재분석 트리거
     */
    @Transactional
    public SalesNote updateNote(Long id, NoteRequestDto dto) {
        CustomUserDetails userDetails = getCurrentUserDetails();
        SalesNote note = noteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 노트를 찾을 수 없습니다. ID: " + id));

        if (!note.getAuthorId().equals(userDetails.getEmployeeId())) {
            throw new AccessDeniedException("작성자만 수정/삭제할 수 있습니다.");
        }

        validateContractOwnership(dto.getContractId(), note.getClientId());

        List<String> summary = aiClient.summarizeNote(dto.getContent());
        note.updateNote(dto.getContent(), dto.getContractId(), dto.getActivityDate(), summary);

        // [RAG] 변경된 내용 벡터 DB 재인덱싱 (트랜잭션 커밋 후)
        triggerRagIndexingAfterCommit(note);

        // 비동기 분석 요청
        triggerBriefingUpdate(note.getClientId());

        return note;
    }

    /**
     * 4. 영업 활동 기록 삭제 및 AI 재분석 트리거
     */
    @Transactional
    public void deleteNote(Long id) {
        CustomUserDetails userDetails = getCurrentUserDetails();
        SalesNote note = noteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제할 노트를 찾을 수 없습니다."));

        if (!note.getAuthorId().equals(userDetails.getEmployeeId())) {
            throw new AccessDeniedException("작성자만 수정/삭제할 수 있습니다.");
        }

        Long clientId = note.getClientId();
        Long noteId = note.getId();
        
        noteRepository.delete(note);

        // [RAG] 벡터 DB에서 정보 삭제 (트랜잭션 커밋 후)
        triggerRagDeletionAfterCommit(noteId);

        // 비동기 분석 요청
        triggerBriefingUpdate(clientId);
    }

    /**
     * [리팩토링] RAG 인덱싱 비동기/지연 처리 헬퍼
     * 트랜잭션이 성공적으로 커밋된 후에만 벡터 DB에 반영하여 정합성을 유지합니다.
     */
    private void triggerRagIndexingAfterCommit(SalesNote note) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        log.info("트랜잭션 커밋 완료 확인 - RAG 인덱싱 수행: Note ID={}", note.getId());
                        ragService.indexNote(note); // 내부적으로 삭제 후 등록 처리됨
                    } catch (Exception e) {
                        log.error("[RAG] 트랜잭션 후 인덱싱 실패 (Note ID: {}): {}", note.getId(), e.getMessage());
                    }
                }
            });
        } else {
            try {
                ragService.indexNote(note);
            } catch (Exception e) {
                log.error("[RAG] 인덱싱 실패 (Note ID: {}): {}", note.getId(), e.getMessage());
            }
        }
    }

    /**
     * [리팩토링] RAG 삭제 비동기/지연 처리 헬퍼
     */
    private void triggerRagDeletionAfterCommit(Long noteId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("트랜잭션 커밋 완료 확인 - RAG 데이터 삭제: Note ID={}", noteId);
                    ragService.deleteNote(noteId);
                }
            });
        } else {
            ragService.deleteNote(noteId);
        }
    }

    /**
     * 계약 소유권 검증 헬퍼 메서드
     */
    private void validateContractOwnership(String contractCode, Long clientId) {
        if (contractCode != null && !contractCode.isBlank()) {
            boolean exists = contractRepository.existsByContractCodeAndClientId(contractCode, clientId);
            if (!exists) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    String.format("해당 거래처(ID: %d)에 귀속된 계약 코드(%s)를 찾을 수 없습니다.", clientId, contractCode));
            }
        }
    }

    /**
     * 비동기 분석 트리거
     */
    private void triggerBriefingUpdate(Long clientId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("트랜잭션 커밋 완료 확인 - RAGseed 분석 트리거: clientId={}", clientId);
                    try {
                        ragSeedService.refreshStandardBriefingAsync(clientId);
                    } catch (RejectedExecutionException e) {
                        log.error("[RAGseed] 비동기 분석 작업 제출 거부됨 (Executor 포화): clientId={}", clientId);
                    }
                }
            });
        } else {
            try {
                ragSeedService.refreshStandardBriefingAsync(clientId);
            } catch (RejectedExecutionException e) {
                log.error("[RAGseed] 비동기 분석 작업 제출 거부됨 (Executor 포화): clientId={}", clientId);
            }
        }
    }

    private CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new IllegalStateException("인증 정보를 찾을 수 없습니다.");
        }
        return userDetails;
    }

    private Long getCurrentEmployeeId() {
        return getCurrentUserDetails().getEmployeeId();
    }
}
