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

        // [리팩토링] 역할별 권한 검증 (AccountService 패턴 적용)
        if (role == Role.ADMIN) {
            // 관리자는 모든 거래처에 대해 노트 작성이 가능하며, 거래처 존재 여부만 확인
            if (!clientRepository.existsById(dto.getClientId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 거래처입니다.");
            }
        } else if (role == Role.SALES_REP) {
            // 영업사원은 본인이 담당하는 거래처인지 확인
            Client client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 거래처입니다."));

            if (client.getManagerEmployee() == null || !client.getManagerEmployee().getId().equals(currentEmployeeId)) {
                throw new AccessDeniedException("본인이 담당하는 거래처의 노트만 작성할 수 있습니다.");
            }
        } else {
            // 그 외 역할(CLIENT 등)은 영업 활동 기록 작성 불가
            throw new AccessDeniedException("노트 작성 권한이 없습니다.");
        }

        // [추가] 계약 ID 유효성 검증: 해당 거래처의 계약인지 확인
        validateContractOwnership(dto.getContractId(), dto.getClientId());

        // [자동화] 저장 전 실시간 AI 요약 생성
        List<String> summary = aiClient.summarizeNote(dto.getContent());

        SalesNote note = SalesNote.builder()
                .clientId(dto.getClientId())
                .authorId(currentEmployeeId)
                .contractId(dto.getContractId())
                .activityDate(dto.getDate())
                .content(dto.getContent())
                .aiSummary(summary) // AI 요약 결과 직접 주입
                .build();
        
        SalesNote savedNote = noteRepository.save(note);

        // [RAG] 벡터 DB 인덱싱 수행
        ragService.indexNote(savedNote);

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 노트를 찾을 수 없습니다. ID: " + id));

        // 본인 작성 노트가 아닌 경우 거부 (ADMIN 포함 모든 역할 적용)
        if (!note.getAuthorId().equals(userDetails.getEmployeeId())) {
            throw new AccessDeniedException("작성자만 수정/삭제할 수 있습니다.");
        }

        // [추가] 계약 ID 유효성 검증: 해당 거래처의 계약인지 확인 (수정 시에도 체크)
        // 기존 노트의 clientId를 기준으로 검증하여 데이터 변조 방지
        validateContractOwnership(dto.getContractId(), note.getClientId());

        // [자동화] 수정 시 내용이 바뀌었을 수 있으므로 AI 요약 재신청
        List<String> summary = aiClient.summarizeNote(dto.getContent());
        note.updateNote(dto.getContent(), dto.getContractId(), dto.getActivityDate(), summary);

        // [RAG] 변경된 내용 벡터 DB 재인덱싱 (기존 InMemoryStore는 중복 저장을 방지하는 로직이 필요할 수 있으나 여기선 단순화)
        ragService.indexNote(note);

        // [리팩토링] 데이터 수정 후 비동기 분석 요청
        triggerBriefingUpdate(note.getClientId());

        return note;
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
     * 4. 영업 활동 기록 삭제 및 AI 재분석 트리거
     */
    @Transactional
    public void deleteNote(Long id) {
        CustomUserDetails userDetails = getCurrentUserDetails();
        SalesNote note = noteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제할 노트를 찾을 수 없습니다."));

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
                    log.info("트랜잭션 커밋 완료 확인 - RAGseed 분석 트리거: clientId={}", clientId);
                    ragSeedService.refreshStandardBriefingAsync(clientId);
                }
            });
        } else {
            // 트랜잭션이 없는 환경(예: 테스트 코드 등)에서는 즉시 호출
            ragSeedService.refreshStandardBriefingAsync(clientId);
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