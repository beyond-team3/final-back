package com.monsoon.seedflowplus.domain.scoring.service;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import com.monsoon.seedflowplus.domain.note.repository.SalesNoteRepository;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.order.repository.OrderHeaderRepository;
import com.monsoon.seedflowplus.domain.scoring.dto.AccountPriorityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoringService {

    private final ClientRepository clientRepository;
    private final ContractRepository contractRepository;
    private final OrderHeaderRepository orderHeaderRepository;
    private final SalesNoteRepository salesNoteRepository;

    public List<AccountPriorityResponse> getRankedAccounts() {
        return clientRepository.findAll().stream()
                .map(this::calculateAccountScore)
                .sorted(Comparator.comparing(AccountPriorityResponse::totalScore).reversed())
                .toList();
    }

    private AccountPriorityResponse calculateAccountScore(Client client) {
        LocalDate today = LocalDate.now();
        
        List<LocalDate> endDates = contractRepository.findByClientOrderByEndDateAsc(client).stream()
                .map(ContractHeader::getEndDate)
                .toList();

        LocalDate expiryDate = endDates.stream()
                .filter(date -> !date.isBefore(today)) // 오늘 이후 계약 중 가장 가까운 만료일
                .findFirst()
                .orElseGet(() -> endDates.stream()
                        .filter(date -> date.isBefore(today)) // 없을 경우 과거 계약 중 가장 최근 만료일
                        .reduce((first, second) -> second)
                        .orElse(null));

        boolean hasOrder = orderHeaderRepository.existsByClient(client);

        LocalDate lastVisit = salesNoteRepository.findTopByClientIdOrderByActivityDateDesc(client.getId())
                .map(SalesNote::getActivityDate)
                .orElse(null);

        double cScore = calculateContractScore(expiryDate, today);
        double oScore = hasOrder ? 0.0 : 50.0;
        double vScore = calculateVisitScore(lastVisit, today);

        double total = (cScore * 0.5) + (oScore * 0.3) + (vScore * 0.2);

        return createResponse(client, total, cScore, oScore, vScore);
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

    private AccountPriorityResponse createResponse(Client client, double total, double c, double o, double v) {
        String reason;
        String detail;

        double cWeight = c * 0.5;
        double oWeight = o * 0.3;
        double vWeight = v * 0.2;

        if (cWeight >= oWeight && cWeight >= vWeight) {
            reason = "계약 종료 임박";
            detail = "계약 종료가 30일 이내로 다가와 재계약 논의가 시급합니다.";
        } else if (oWeight >= vWeight) {
            reason = "주문 공백 발생";
            detail = "현재 진행 중인 주문서가 확인되지 않아 신규 주문 유도가 필요합니다.";
        } else {
            reason = "장기 미방문";
            detail = "마지막 방문 이후 상당 기간이 경과하여 관계 유지를 위한 방문을 권장합니다.";
        }

        return AccountPriorityResponse.builder()
                .accountId(client.getId())
                .accountName(client.getClientName())
                .totalScore(Math.round(total * 100) / 100.0)
                .primaryReason(reason)
                .detailDescription(detail)
                .breakdown(new AccountPriorityResponse.ScoreBreakdown(c, o, v))
                .build();
    }
}
