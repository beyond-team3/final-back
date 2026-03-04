package com.monsoon.seedflowplus.domain.scoring.service;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.note.repository.SalesNoteRepository;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.order.repository.OrderHeaderRepository;
import com.monsoon.seedflowplus.domain.scoring.dto.AccountPriorityResponse;
import com.monsoon.seedflowplus.domain.scoring.entity.AccountScore;
import com.monsoon.seedflowplus.domain.scoring.repository.AccountScoreRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ScoringService {

    private final ClientRepository clientRepository;
    private final ContractRepository contractRepository;
    private final OrderHeaderRepository orderHeaderRepository;
    private final SalesNoteRepository salesNoteRepository;
    private final AccountScoreRepository accountScoreRepository;

    public List<AccountPriorityResponse> getRankedAccounts() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        
        List<Client> clients;
        if (userDetails.getRole() == Role.ADMIN) {
            clients = clientRepository.findAll();
        } else if (userDetails.getRole() == Role.SALES_REP) {
            clients = clientRepository.findAllByManagerEmployeeId(userDetails.getEmployeeId());
        } else {
            return List.of(); // 다른 역할은 빈 목록 반환
        }

        // N+1 최적화: 벌크 데이터 사전 조회
        Map<Long, List<LocalDate>> endDatesMap = contractRepository.findAllClientEndDates();
        Set<Long> clientsWithOrders = orderHeaderRepository.findAllClientIdsWithOrders();
        Map<Long, LocalDate> lastVisitsMap = salesNoteRepository.findAllLastActivityDates();

        return clients.stream()
                .map(client -> calculateAndPersistScore(client, endDatesMap, clientsWithOrders, lastVisitsMap))
                .sorted(Comparator.comparing(AccountPriorityResponse::totalScore).reversed())
                .toList();
    }

    private AccountPriorityResponse calculateAndPersistScore(Client client,
                                                           Map<Long, List<LocalDate>> endDatesMap,
                                                           Set<Long> clientsWithOrders,
                                                           Map<Long, LocalDate> lastVisitsMap) {
        LocalDate today = LocalDate.now();
        Long clientId = client.getId();

        // 1. 데이터 조회 (메모리 맵 활용)
        List<LocalDate> endDates = endDatesMap.getOrDefault(clientId, List.of());
        boolean hasOrder = clientsWithOrders.contains(clientId);
        LocalDate lastVisit = lastVisitsMap.get(clientId);

        // 계약 종료일 추출 로직 (미래 우선 / 과거 fallback)
        LocalDate expiryDate = endDates.stream()
                .filter(date -> !date.isBefore(today))
                .findFirst()
                .orElseGet(() -> endDates.stream()
                        .filter(date -> date.isBefore(today))
                        .reduce((first, second) -> second)
                        .orElse(null));

        // 2. 점수 계산
        double cScore = calculateContractScore(expiryDate, today);
        double oScore = hasOrder ? 0.0 : 50.0;
        double vScore = calculateVisitScore(lastVisit, today);

        double total = Math.round(((cScore * 0.5) + (oScore * 0.3) + (vScore * 0.2)) * 100) / 100.0;

        // 3. 근거 및 설명 생성
        String[] reasons = generateReasons(total, cScore, oScore, vScore);
        String reason = reasons[0];
        String detail = reasons[1];

        // 4. DB 영속화 (AccountScore 엔티티 활용)
        accountScoreRepository.findByClient_Id(clientId)
                .ifPresentOrElse(
                        existingScore -> existingScore.updateScore(total, cScore, oScore, vScore, reason, detail),
                        () -> accountScoreRepository.save(AccountScore.builder()
                                .client(client)
                                .totalScore(total)
                                .contractScore(cScore)
                                .orderScore(oScore)
                                .visitScore(vScore)
                                .primaryReason(reason)
                                .detailDescription(detail)
                                .build())
                );

        return AccountPriorityResponse.builder()
                .accountId(client.getId())
                .accountName(client.getClientName())
                .totalScore(total)
                .primaryReason(reason)
                .detailDescription(detail)
                .breakdown(new AccountPriorityResponse.ScoreBreakdown(cScore, oScore, vScore))
                .build();
    }

    private double calculateContractScore(LocalDate expiry, LocalDate today) {
        if (expiry == null) return 0;
        long daysUntilExpiry = ChronoUnit.DAYS.between(today, expiry);
        
        if (daysUntilExpiry < 0) return 100.0;
        if (daysUntilExpiry > 30) return 0.0;
        
        return Math.pow(30 - daysUntilExpiry, 2) * (100.0 / 900.0);
    }

    private double calculateVisitScore(LocalDate lastVisit, LocalDate today) {
        if (lastVisit == null) return 100.0;
        long daysSinceVisit = ChronoUnit.DAYS.between(lastVisit, today);
        return Math.max(0.0, Math.min(100.0, daysSinceVisit * (100.0 / 90.0)));
    }

    private String[] generateReasons(double total, double c, double o, double v) {
        String reason;
        String detail;

        double cWeight = c * 0.5;
        double oWeight = o * 0.3;
        double vWeight = v * 0.2;

        if (total <= 0) {
            reason = "관리 양호";
            detail = "현재 계약 및 방문 관리가 원활하게 이루어지고 있습니다.";
        } else if (cWeight >= oWeight && cWeight >= vWeight) {
            reason = "계약 종료 임박";
            detail = "계약 종료가 30일 이내로 다가와 재계약 논의가 시급합니다.";
        } else if (oWeight >= vWeight) {
            reason = "주문 공백 발생";
            detail = "현재 진행 중인 주문서가 확인되지 않아 신규 주문 유도가 필요합니다.";
        } else {
            reason = "장기 미방문";
            detail = "마지막 방문 이후 상당 기간이 경과하여 관계 유지를 위한 방문을 권장합니다.";
        }
        
        return new String[]{reason, detail};
    }

    private CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new IllegalStateException("인증 정보를 찾을 수 없습니다.");
        }
        return userDetails;
    }
}
