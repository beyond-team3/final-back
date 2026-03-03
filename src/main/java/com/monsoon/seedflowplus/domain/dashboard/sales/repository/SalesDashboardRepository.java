package com.monsoon.seedflowplus.domain.dashboard.sales.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public class SalesDashboardRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public SalesDashboardRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ──────────────────────────────────────────────
    // 0. 영업사원 이름 조회
    // ──────────────────────────────────────────────

    /**
     * 사원이 없으면 NoSuchElementException → 서비스에서 404/401 변환 가능
     */
    public String findEmployeeName(Long employeeId) {
        String sql = """
                SELECT employee_name
                FROM tbl_employee
                WHERE employee_id = :empId
                """;
        List<String> results = jdbc.queryForList(sql,
                new MapSqlParameterSource().addValue("empId", employeeId),
                String.class);
        return results.stream()
                .findFirst()
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "Employee not found: " + employeeId));
    }

    // ──────────────────────────────────────────────
    // 1. 월별 매출 (PAID 청구서 기준)
    // ──────────────────────────────────────────────

    /**
     * 특정 월의 PAID 매출 합계
     * @param employeeId 영업사원 PK
     * @param start      해당 월 1일
     * @param end        해당 월 말일
     */
    public BigDecimal sumPaidInvoice(Long employeeId, LocalDate start, LocalDate end) {
        String sql = """
                SELECT COALESCE(SUM(i.total_amount), 0)
                FROM tbl_invoice i
                WHERE i.employee_id = :empId
                  AND i.status      = 'PAID'
                  AND i.invoice_date BETWEEN :start AND :end
                """;
        return jdbc.queryForObject(sql,
                new MapSqlParameterSource()
                        .addValue("empId", employeeId)
                        .addValue("start", start)
                        .addValue("end", end),
                BigDecimal.class);
    }

    /**
     * 최근 N개월치 월별 PAID 매출 집계 (바 차트용)
     * @param employeeId 영업사원 PK
     * @param fromDate   조회 시작일 (N개월 전 1일)
     * @param toDate     조회 종료일 (이번 달 말일)
     * @return [{year_month: "2026-03", total: 3560000.00}, ...]
     */
    public List<Map<String, Object>> monthlyPaidSales(Long employeeId,
                                                      LocalDate fromDate,
                                                      LocalDate toDate) {
        String sql = """
                SELECT DATE_FORMAT(i.invoice_date, '%Y-%m') AS year_month,
                       COALESCE(SUM(i.total_amount), 0)     AS total
                FROM tbl_invoice i
                WHERE i.employee_id  = :empId
                  AND i.status       = 'PAID'
                  AND i.invoice_date BETWEEN :from AND :to
                GROUP BY DATE_FORMAT(i.invoice_date, '%Y-%m')
                ORDER BY year_month
                """;
        return jdbc.queryForList(sql,
                new MapSqlParameterSource()
                        .addValue("empId", employeeId)
                        .addValue("from", fromDate)
                        .addValue("to", toDate));
    }

    // ──────────────────────────────────────────────
    // 2. 완료 계약 건수
    // ──────────────────────────────────────────────

    /**
     * 이번 달 COMPLETED 계약 건수
     * issue_date(계약 체결일) 기준
     */
    public int countCompletedContracts(Long employeeId, LocalDate start, LocalDate end) {
        String sql = """
                SELECT COUNT(*)
                FROM tbl_contract_header c
                WHERE c.author_id  = :empId
                  AND c.status     = 'COMPLETED'
                  AND DATE(c.issue_date) BETWEEN :start AND :end
                """;
        return jdbc.queryForObject(sql,
                new MapSqlParameterSource()
                        .addValue("empId", employeeId)
                        .addValue("start", start)
                        .addValue("end", end),
                Integer.class);
    }

    // ──────────────────────────────────────────────
    // 3. 이번 달 청구 대상 계약
    //    billing_cycle 기준으로 이번 달이 청구 도래 시점인 계약 조회
    //    → 이미 CANCELED 된 invoice 는 제외
    //    → DRAFT / PUBLISHED 상태 invoice 포함 (미발행 포함)
    // ──────────────────────────────────────────────

    /**
     * billing_cycle 계산 방식
     *  MONTHLY    : start_date 기준 매월 도래
     *  QUARTERLY  : start_date 기준 3개월마다 도래
     *  HALF_YEARLY: start_date 기준 6개월마다 도래
     *
     * "이번 달이 청구 도래 월" 조건:
     *  DATEDIFF 대신 PERIOD_DIFF(이번달, start_date 월) % cycle_months = 0
     */
    public List<Map<String, Object>> billingTargetsThisMonth(Long employeeId,
                                                             LocalDate thisMonthStart,
                                                             LocalDate thisMonthEnd) {
        String sql = """
                SELECT doc_no, client_name, total_amount, invoice_date, status
                FROM (
                    SELECT i.invoice_code                          AS doc_no,
                           cl.client_name,
                           i.total_amount,
                           i.invoice_date,
                           i.status,
                           ROW_NUMBER() OVER (
                               PARTITION BY c.cnt_id
                               ORDER BY i.invoice_date DESC
                           ) AS rn
                    FROM tbl_contract_header c
                    JOIN tbl_client cl ON cl.client_id = c.client_id
                    LEFT JOIN tbl_invoice i
                           ON i.contract_id  = c.cnt_id
                          AND i.employee_id  = :empId
                          AND i.status      != 'CANCELED'
                          AND i.invoice_date BETWEEN :start AND :end
                    WHERE c.author_id = :empId
                      AND c.status NOT IN ('DELETED', 'EXPIRED', 'REJECTED_ADMIN', 'REJECTED_CLIENT')
                      AND c.start_date <= :end
                      AND (c.end_date IS NULL OR c.end_date >= :start)
                      AND (
                          c.billing_cycle = 'MONTHLY'
                          OR (c.billing_cycle = 'QUARTERLY'
                              AND MOD(PERIOD_DIFF(DATE_FORMAT(:start, '%Y%m'),
                                                  DATE_FORMAT(c.start_date, '%Y%m')), 3) = 0)
                          OR (c.billing_cycle = 'HALF_YEARLY'
                              AND MOD(PERIOD_DIFF(DATE_FORMAT(:start, '%Y%m'),
                                                  DATE_FORMAT(c.start_date, '%Y%m')), 6) = 0)
                      )
                ) ranked
                WHERE rn = 1
                ORDER BY invoice_date ASC, client_name ASC
                """;
        return jdbc.queryForList(sql,
                new MapSqlParameterSource()
                        .addValue("empId", employeeId)
                        .addValue("start", thisMonthStart)
                        .addValue("end", thisMonthEnd));
    }

    // ──────────────────────────────────────────────
    // 4. 최근 영업 히스토리
    // ──────────────────────────────────────────────

    /**
     * 본인이 actor 인 최근 활동 5건
     * tbl_sales_history + tbl_client (client_name)
     */
    public List<Map<String, Object>> recentActivities(Long employeeId) {
        String sql = """
                SELECT sh.action_datetime,
                       sh.doc_type,
                       sh.action_type,
                       sh.target_code,
                       sh.to_stage,
                       cl.client_name
                FROM tbl_sales_history sh
                JOIN tbl_client cl ON cl.client_id = sh.client_id
                WHERE sh.actor_emp_id = :empId
                ORDER BY sh.action_datetime DESC
                LIMIT 5
                """;
        return jdbc.queryForList(sql,
                new MapSqlParameterSource().addValue("empId", employeeId));
    }
}