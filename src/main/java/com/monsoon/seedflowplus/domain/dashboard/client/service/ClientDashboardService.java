package com.monsoon.seedflowplus.domain.dashboard.client.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.dashboard.client.dto.*;
import com.monsoon.seedflowplus.domain.dashboard.client.repository.ClientDashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientDashboardService {

    private final ClientDashboardRepository repo;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ClientDashboardResponse getDashboard(Long clientId, Long userId) {

        // ── 1. 거래처 기본정보 + 청구 사이클 ──────────
        Map<String, Object> clientInfo;
        try {
            clientInfo = repo.findClientInfo(clientId);
        } catch (java.util.NoSuchElementException e) {
            throw new CoreException(ErrorType.CLIENT_NOT_FOUND);
        }
        String clientName   = (String) clientInfo.get("client_name");
        String billingCycle = (String) clientInfo.get("billing_cycle");
        LocalDate startDate = clientInfo.get("start_date") != null
                ? ((java.sql.Date) clientInfo.get("start_date")).toLocalDate()
                : LocalDate.now();

        // ── 2. 최근 주문 ───────────────────────────
        List<Map<String, Object>> rawOrders = repo.recentOrders(clientId);
        List<ClientOrderResponse> orders = rawOrders.stream()
                .map(this::toOrderResponse)
                .toList();

        // ── 3. 미결제 청구서 ────────────────────────
        List<Map<String, Object>> rawBillings = repo.unpaidInvoices(clientId);
        List<ClientBillingResponse> billings = rawBillings.stream()
                .map(this::toBillingResponse)
                .toList();

        // ── 4. 최근 알림 ────────────────────────────
        List<Map<String, Object>> rawNotifs = repo.recentNotifications(userId);
        List<ClientNotificationResponse> notifications = rawNotifs.stream()
                .map(this::toNotificationResponse)
                .toList();

        // ── 5. 조립 ────────────────────────────────
        LocalDate today = LocalDate.now();
        return ClientDashboardResponse.builder()
                .title(clientName + " 거래 현황")
                .subtitle(today.getYear() + "년 " + today.getMonthValue() + "월 기준")
                .billingCycle(buildBillingCycle(billingCycle, startDate, today))
                .orders(orders)
                .billings(billings)
                .notifications(notifications)
                .build();
    }

    // ──────────────────────────────────────────────
    // 청구 사이클 레이블 + 다음 청구일 계산
    // ──────────────────────────────────────────────

    private BillingCycleResponse buildBillingCycle(String cycle, LocalDate startDate, LocalDate today) {
        String value;
        LocalDate next;

        if (cycle == null) {
            return BillingCycleResponse.builder().value("-").next("-").build();
        }

        switch (cycle) {
            case "MONTHLY" -> {
                value = "월별 청구";
                next  = today.withDayOfMonth(1).plusMonths(1);
            }
            case "QUARTERLY" -> {
                value = "분기별 청구";
                next  = calcNextCycleDate(startDate, today, 3);
            }
            case "HALF_YEARLY" -> {
                value = "반기별 청구";
                next  = calcNextCycleDate(startDate, today, 6);
            }
            default -> {
                value = cycle;
                next  = today.plusMonths(1);
            }
        }

        return BillingCycleResponse.builder()
                .value(value)
                .next("다음 청구일 " + next.format(DATE_FMT))
                .build();
    }

    /**
     * start_date 기준으로 cycleMonths 단위 순환 중
     * today 이후 첫 번째 도래일 계산
     */
    private LocalDate calcNextCycleDate(LocalDate startDate, LocalDate today, int cycleMonths) {
        LocalDate candidate = startDate;
        while (!candidate.isAfter(today)) {
            candidate = candidate.plusMonths(cycleMonths);
        }
        return candidate;
    }

    // ──────────────────────────────────────────────
    // 주문 → DTO 변환
    // ──────────────────────────────────────────────

    private ClientOrderResponse toOrderResponse(Map<String, Object> row) {
        String statusRaw = row.get("status") != null ? (String) row.get("status") : "UNKNOWN";
        String status;
        String statusClass;

        switch (statusRaw) {
            case "PENDING"   -> { status = "대기";    statusClass = "pending"; }
            case "CONFIRMED" -> { status = "처리 중"; statusClass = "processing"; }
            default          -> { status = "완료";    statusClass = "completed"; }
        }

        LocalDateTime createdAt = toLocalDateTime(row.get("created_at"));

        String rawSummary = row.get("summary") != null ? (String) row.get("summary") : "-";

        return ClientOrderResponse.builder()
                .no((String) row.get("order_code"))
                .date(createdAt.toLocalDate().format(DATE_FMT))
                .status(status)
                .statusClass(statusClass)
                .summary(rawSummary)
                .amount(formatAmount((BigDecimal) row.get("total_amount")))
                .action("상세 보기")
                .build();
    }

    // ──────────────────────────────────────────────
    // 청구서 → DTO 변환
    // ──────────────────────────────────────────────

    private ClientBillingResponse toBillingResponse(Map<String, Object> row) {
        String statusRaw = (String) row.get("status");
        LocalDate invoiceDate =
                ((java.sql.Date) row.get("invoice_date")).toLocalDate();
        LocalDate today = LocalDate.now();

        String status = "PUBLISHED".equals(statusRaw) ? "납부 완료" : "미결제";

        // 마감 7일 이내 → due-soon, PUBLISHED → paid
        String type;
        if ("PUBLISHED".equals(statusRaw)) {
            type = "paid";
        } else if (!invoiceDate.isBefore(today) && invoiceDate.isBefore(today.plusDays(8))) {
            type = "due-soon";
        } else {
            type = "";
        }

        return ClientBillingResponse.builder()
                .no((String) row.get("invoice_code"))
                .due(invoiceDate.format(DATE_FMT) + " 마감")
                .amount(formatAmount((BigDecimal) row.get("total_amount")))
                .status(status)
                .type(type)
                .build();
    }

    // ──────────────────────────────────────────────
    // 알림 → DTO 변환
    // ──────────────────────────────────────────────

    private ClientNotificationResponse toNotificationResponse(Map<String, Object> row) {
        LocalDateTime createdAt = toLocalDateTime(row.get("created_at"));
        String content = (String) row.get("content");
        String detail  = content != null && content.length() > 50
                ? content.substring(0, 50) + "…"
                : content;

        return ClientNotificationResponse.builder()
                .time(createdAt.toLocalDate().format(DATE_FMT))
                .title((String) row.get("title"))
                .detail(detail)
                .isNew(row.get("read_at") == null)
                .build();
    }

    // ──────────────────────────────────────────────
    // 포맷 헬퍼
    // ──────────────────────────────────────────────

    private String formatAmount(BigDecimal value) {
        if (value == null) return "0원";
        return NumberFormat.getInstance(Locale.KOREA).format(value.longValue()) + "원";
    }

    // ──────────────────────────────────────────────
    // 타입 안전 Timestamp 변환
    // ──────────────────────────────────────────────

    private LocalDateTime toLocalDateTime(Object raw) {
        if (raw instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (raw instanceof LocalDateTime ldt) return ldt;
        throw new IllegalStateException("변환할 수 없는 날짜 타입: "
                + (raw == null ? "null" : raw.getClass().getName()));
    }
}