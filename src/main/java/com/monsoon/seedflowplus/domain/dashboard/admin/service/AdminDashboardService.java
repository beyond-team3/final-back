package com.monsoon.seedflowplus.domain.dashboard.admin.service;

import com.monsoon.seedflowplus.domain.dashboard.admin.dto.*;
import com.monsoon.seedflowplus.domain.dashboard.admin.repository.AdminDashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final AdminDashboardRepository repo;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // doc_type → 한글
    private static final Map<String, String> DOC_LABEL = Map.of(
            "QUO", "견적", "CNT", "계약", "ORD", "주문",
            "INV", "청구", "RFQ", "견적 요청", "STMT", "명세서",
            "PAY", "결제"
    );

    public AdminDashboardResponse getDashboard() {

        LocalDate today          = LocalDate.now();
        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate thisMonthEnd   = today.withDayOfMonth(today.lengthOfMonth());

        int thisYear = today.getYear();
        int lastYear = thisYear - 1;

        // ── 1. KPI ────────────────────────────────
        BigDecimal thisMonthSales = repo.sumTotalPaidSales(thisMonthStart, thisMonthEnd);

        // 전년 동월
        LocalDate lastYearStart = thisMonthStart.minusYears(1);
        LocalDate lastYearEnd   = thisMonthEnd.minusYears(1);
        BigDecimal lastYearSameSales = repo.sumTotalPaidSales(lastYearStart, lastYearEnd);

        double growthRate = calcGrowthRate(thisMonthSales, lastYearSameSales);

        List<Map<String, Object>> pendingRows = repo.countPendingByDocType();
        KpiResponse kpis = buildKpis(thisMonthSales, growthRate, pendingRows);

        // ── 2. 매출 추이 차트 ──────────────────────
        SalesTrendResponse salesTrend = buildSalesTrend(lastYear, thisYear, today.getMonthValue());

        // ── 3. 영업사원 매출 랭킹 ──────────────────
        List<Map<String, Object>> rawRankings = repo.salesRankingThisMonth(thisMonthStart, thisMonthEnd);
        List<SalesRankingResponse> rankings = buildRankings(rawRankings);

        // ── 4. 최근 승인 요청 ──────────────────────
        List<Map<String, Object>> rawApprovals = repo.recentApprovalRequests();
        List<ApprovalRequestResponse> approvals = rawApprovals.stream()
                .map(this::toApprovalResponse)
                .toList();

        // ── 5. 조립 ────────────────────────────────
        return AdminDashboardResponse.builder()
                .title("관리자 대시보드")
                .trendPeriod(lastYear + "년 – " + thisYear + "년 월별 매출 추이")
                .kpis(kpis)
                .salesTrend(salesTrend)
                .rankings(rankings)
                .approvalCount(approvals.size())
                .approvals(approvals)
                .build();
    }

    // ──────────────────────────────────────────────
    // KPI 조립
    // ──────────────────────────────────────────────

    private KpiResponse buildKpis(BigDecimal thisMonthSales,
                                  double growthRate,
                                  List<Map<String, Object>> pendingRows) {
        // 승인 대기 유형별 집계
        long quoCount = 0, cntCount = 0, ordCount = 0, totalPending = 0;
        for (Map<String, Object> row : pendingRows) {
            String type = (String) row.get("doc_type");
            long   cnt  = ((Number) row.get("cnt")).longValue();
            totalPending += cnt;
            switch (type) {
                case "QUO" -> quoCount = cnt;
                case "CNT" -> cntCount = cnt;
                case "ORD" -> ordCount = cnt;
            }
        }

        long otherCount = totalPending - (quoCount + cntCount + ordCount);
        String pendingDetail = otherCount > 0
                ? String.format("견적 %d / 계약 %d / 주문 %d / 기타 %d", quoCount, cntCount, ordCount, otherCount)
                : String.format("견적 %d / 계약 %d / 주문 %d", quoCount, cntCount, ordCount);

        return KpiResponse.builder()
                .totalMonthlySales(formatEok(thisMonthSales))
                .salesGrowthRate(formatGrowthRate(growthRate))
                .pendingDocumentCount(totalPending + "건")
                .pendingDetail(pendingDetail)
                .build();
    }

    // ──────────────────────────────────────────────
    // 매출 추이 — 12개월 슬롯, 미래 월은 null
    // ──────────────────────────────────────────────

    private SalesTrendResponse buildSalesTrend(int lastYear, int thisYear, int currentMonth) {
        List<Map<String, Object>> lastRows = repo.monthlyPaidSalesByYear(lastYear);
        List<Map<String, Object>> thisRows = repo.monthlyPaidSalesByYear(thisYear);

        // month → total 맵으로 변환
        Map<Integer, Long> lastMap = toMonthMap(lastRows);
        Map<Integer, Long> thisMap = toMonthMap(thisRows);

        List<Long> lastYearData = new ArrayList<>();
        List<Long> thisYearData = new ArrayList<>();

        for (int m = 1; m <= 12; m++) {
            lastYearData.add(lastMap.getOrDefault(m, 0L));
            // 올해 미래 월은 null (차트에서 spanGaps: false 처리)
            thisYearData.add(m <= currentMonth ? thisMap.getOrDefault(m, 0L) : null);
        }

        return SalesTrendResponse.builder()
                .lastYear(lastYearData)
                .thisYear(thisYearData)
                .build();
    }

    private Map<Integer, Long> toMonthMap(List<Map<String, Object>> rows) {
        Map<Integer, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            int  month = ((Number) row.get("month")).intValue();
            // DB 결과는 원 단위 → 만원 단위로 변환 (차트 단위 맞춤)
            long total = ((BigDecimal) row.get("total")).divide(BigDecimal.valueOf(10000),
                    0, RoundingMode.HALF_UP).longValue();
            map.put(month, total);
        }
        return map;
    }

    // ──────────────────────────────────────────────
    // 랭킹 — 1위 기준 width 100%, 나머지 비례
    // ──────────────────────────────────────────────

    private List<SalesRankingResponse> buildRankings(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return List.of();

        BigDecimal max = (BigDecimal) rows.get(0).get("total");
        if (max.compareTo(BigDecimal.ZERO) == 0) max = BigDecimal.ONE;

        List<SalesRankingResponse> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            BigDecimal total = (BigDecimal) row.get("total");
            int width = total.multiply(BigDecimal.valueOf(100))
                    .divide(max, 0, RoundingMode.HALF_UP)
                    .intValue();

            result.add(SalesRankingResponse.builder()
                    .rank(i + 1)
                    .name((String) row.get("employee_name"))
                    .amount("₩" + NumberFormat.getInstance(Locale.KOREA).format(total.longValue()))
                    .width(width)
                    .employeeId(((Number) row.get("employee_id")).longValue()) // 추가
                    .build());
        }
        return result;
    }

    // ──────────────────────────────────────────────
    // 승인 요청 → DTO 변환
    // ──────────────────────────────────────────────

    private ApprovalRequestResponse toApprovalResponse(Map<String, Object> row) {
        String docType    = (String) row.get("doc_type");
        String clientName = (String) row.get("client_name");
        String actorName  = row.get("actor_name") != null ? (String) row.get("actor_name") : "-";
        Long approvalId   = row.get("approval_id") instanceof Number n ? n.longValue() : null; // 추가

        String docLabel = DOC_LABEL.getOrDefault(docType, docType);
        String title    = docLabel + " 승인 요청 - " + clientName;

        LocalDate actionDate = toLocalDateTime(row.get("action_at")).toLocalDate();

        return ApprovalRequestResponse.builder()
                .title(title)
                .meta(actorName)
                .time(actionDate.format(DATE_FMT))
                .approvalId(approvalId) // 추가
                .build();
    }

    // ──────────────────────────────────────────────
    // 포맷 헬퍼
    // ──────────────────────────────────────────────

    private double calcGrowthRate(BigDecimal current, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return current.subtract(base)
                .divide(base, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private String formatGrowthRate(double rate) {
        if (rate == 0.0) return "-";
        String symbol = rate > 0 ? "▲" : "▼";
        return String.format("%s %.1f%%", symbol, Math.abs(rate));
    }

    /** 억 단위 포맷 — 예: "₩2.4억" */
    private String formatEok(BigDecimal value) {
        if (value == null) return "₩0";
        double eok = value.doubleValue() / 100_000_000.0;
        return String.format("₩%.1f억", eok);
    }

    // ──────────────────────────────────────────────
    // 타입 안전 Timestamp 변환
    // ──────────────────────────────────────────────

    private java.time.LocalDateTime toLocalDateTime(Object raw) {
        if (raw instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (raw instanceof java.time.LocalDateTime ldt) return ldt;
        throw new IllegalStateException("변환할 수 없는 날짜 타입: "
                + (raw == null ? "null" : raw.getClass().getName()));
    }
}