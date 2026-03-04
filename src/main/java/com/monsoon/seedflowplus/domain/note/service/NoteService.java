package com.monsoon.seedflowplus.domain.note.service;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import com.monsoon.seedflowplus.domain.note.repository.SalesNoteRepository;
import com.monsoon.seedflowplus.domain.note.dto.request.NoteRequestDto;
import com.monsoon.seedflowplus.domain.note.dto.NoteSearchCondition;
import com.monsoon.seedflowplus.infra.ai.AiClient;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

    private final SalesNoteRepository noteRepository;
    private final ClientRepository clientRepository;
    private final BriefingService briefingService;
    private final AiClient aiClient;

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

        // 영업사원인 경우 본인이 담당하는 거래처인지 확인
        if (userDetails.getRole() == Role.SALES_REP) {
            Client client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래처입니다."));
            
            if (client.getManagerEmployee() == null || !client.getManagerEmployee().getId().equals(currentEmployeeId)) {
                throw new AccessDeniedException("본인이 담당하는 거래처의 노트만 작성할 수 있습니다.");
            }
        }

        // [자동화] 저장 전 실시간 AI 요약 생성
        List<String> summary = aiClient.summarizeNote(dto.getContent());
        dto.setAiSummary(summary);

        SalesNote note = dto.toEntity(currentEmployeeId);
        SalesNote savedNote = noteRepository.save(note);

        // [리팩토링] 비동기 분석 트리거
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
                .orElseThrow(() -> new IllegalArgumentException("해당 노트를 찾을 수 없습니다. ID: " + id));

        // 본인 작성 노트가 아닌 경우 거부 (ADMIN 포함 모든 역할 적용)
        if (!note.getAuthorId().equals(userDetails.getEmployeeId())) {
            throw new AccessDeniedException("작성자만 수정/삭제할 수 있습니다.");
        }

        // [자동화] 수정 시 내용이 바뀌었을 수 있으므로 AI 요약 재신청
        List<String> summary = aiClient.summarizeNote(dto.getContent());
        note.updateNote(dto.getContent(), dto.getContractId(), dto.getActivityDate(), summary);

        // [리팩토링] 데이터 수정 후 비동기 분석 요청
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
                .orElseThrow(() -> new IllegalArgumentException("삭제할 노트를 찾을 수 없습니다."));

        // 본인 작성 노트가 아닌 경우 거부 (ADMIN 포함 모든 역할 적용)
        if (!note.getAuthorId().equals(userDetails.getEmployeeId())) {
            throw new AccessDeniedException("작성자만 수정/삭제할 수 있습니다.");
        }

        Long clientId = note.getClientId();
        noteRepository.delete(note);

        // [리팩토링] 데이터 삭제 후 비동기 분석 요청
        triggerBriefingUpdate(clientId);
    }

    /**
     * [리팩토링] 내부 헬퍼 메서드: 진정한 비동기 호출 수행
     * 메서드명을 의도에 맞게 'trigger'로 변경하고 실제 비동기 메서드를 호출함
     */
    private void triggerBriefingUpdate(Long clientId) {
        // 현재 실행 중인 스레드가 트랜잭션 상태인지 확인
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // 트랜잭션 동기화 매니저에 '커밋 후 실행' 콜백 등록
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 실제 DB 커밋이 완료된 직후에만 비동기 호출 실행
                    log.info("트랜잭션 커밋 완료 확인 - 비동기 분석 트리거: clientId={}", clientId);
                    briefingService.refreshBriefingAsync(clientId);
                }
            });
        } else {
            // 트랜잭션이 없는 환경(예: 테스트 코드 등)에서는 즉시 호출
            briefingService.refreshBriefingAsync(clientId);
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