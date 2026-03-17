package com.monsoon.seedflowplus.domain.dashboard.admin.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public class AdminDashboardRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdminDashboardRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ──────────────────────────────────────────────
    // 1. 특정 월 전체 PAID 매출 합계
    // ──────────────────────────────────────────────

    /**
     * 특정 기간 전체 PAID 매출 합계
     * COALESCE로 데이터 없을 때 0 반환, queryForObject null 방어 추가
     */
    public BigDecimal sumTotalPaidSales(LocalDate start, LocalDate end) {
        String sql = """
                SELECT COALESCE(SUM(total_amount), 0)
                FROM tbl_invoice
                WHERE status = 'PAID'
                  AND invoice_date BETWEEN :start AND :end
                """;
        BigDecimal result = jdbc.queryForObject(sql,
                new MapSqlParameterSource()
                        .addValue("start", start)
                        .addValue("end", end),
                BigDecimal.class);
        return result != null ? result : BigDecimal.ZERO;
    }

    // ──────────────────────────────────────────────
    // 2. 연도별 월별 PAID 매출 (차트용)
    //    → year 인자로 전년 / 올해 각각 호출
    // ──────────────────────────────────────────────

    public List<Map<String, Object>> monthlyPaidSalesByYear(int year) {
        String sql = """
                SELECT MONTH(invoice_date) AS month,
                       COALESCE(SUM(total_amount), 0) AS total
                FROM tbl_invoice
                WHERE status = 'PAID'
                  AND YEAR(invoice_date) = :year
                GROUP BY MONTH(invoice_date)
                ORDER BY month
                """;
        return jdbc.queryForList(sql,
                new MapSqlParameterSource().addValue("year", year));
    }

    // ──────────────────────────────────────────────
    // 3. 승인 대기 문서 수 (유형별)
    //    tbl_sales_deal: current_stage = PENDING_ADMIN
    //    latest_doc_type 기준으로 QUO / CNT / ORD 분류
    // ──────────────────────────────────────────────

    public List<Map<String, Object>> countPendingByDocType() {
        String sql = """
                SELECT latest_doc_type AS doc_type,
                       COUNT(*)        AS cnt
                FROM tbl_sales_deal
                WHERE current_stage = 'PENDING_ADMIN'
                  AND closed_at IS NULL
                GROUP BY latest_doc_type
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource());
    }

    // ──────────────────────────────────────────────
    // 4. 영업사원별 이번 달 PAID 매출 랭킹 (Top 5)
    // ──────────────────────────────────────────────

    public List<Map<String, Object>> salesRankingThisMonth(LocalDate start, LocalDate end) {
        String sql = """
            SELECT e.employee_id,
                   e.employee_name,
                   COALESCE(SUM(i.total_amount), 0) AS total
            FROM tbl_invoice i
            JOIN tbl_employee e ON e.employee_id = i.employee_id
            WHERE i.status = 'PAID'
              AND i.invoice_date BETWEEN :start AND :end
            GROUP BY i.employee_id, e.employee_name
            ORDER BY total DESC
            LIMIT 5
            """;
        return jdbc.queryForList(sql,
                new MapSqlParameterSource()
                        .addValue("start", start)
                        .addValue("end", end));
    }

    // ──────────────────────────────────────────────
    // 5. 최근 승인 요청 (PENDING_ADMIN 로그, 최근 10건)
    //    tbl_sales_deal_log + tbl_employee(요청자) + tbl_client
    // ──────────────────────────────────────────────

    public List<Map<String, Object>> recentApprovalRequests() {
        String sql = """
            SELECT dl.doc_type,
                   dl.target_code,
                   dl.action_at,
                   e.employee_name  AS actor_name,
                   cl.client_name,
                   a.approval_id
            FROM tbl_sales_deal_log dl
            JOIN tbl_sales_deal d  ON d.deal_id    = dl.deal_id
            JOIN tbl_client cl     ON cl.client_id  = dl.client_id
            LEFT JOIN tbl_employee e ON e.employee_id = dl.actor_id
            LEFT JOIN (
                SELECT target_code_snapshot,
                       MAX(approval_request_id) AS approval_id
                FROM tbl_approval_request
                GROUP BY target_code_snapshot
            ) a ON a.target_code_snapshot = dl.target_code
            WHERE dl.to_stage = 'PENDING_ADMIN'
            ORDER BY dl.action_at DESC
            LIMIT 10
            """;
        return jdbc.queryForList(sql, new MapSqlParameterSource());
    }
}