package com.monsoon.seedflowplus.domain.dashboard.client.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ClientDashboardRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ClientDashboardRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ──────────────────────────────────────────────
    // 1. 거래처 기본정보 (이름 + 청구 사이클)
    //    사이클은 해당 거래처의 활성 계약 중 가장 최근 것 기준
    // ──────────────────────────────────────────────

    /**
     * 거래처 기본정보 + 최근 활성 계약의 청구 사이클
     * 결과 없으면 NoSuchElementException → Service에서 CoreException 변환
     */
    public Map<String, Object> findClientInfo(Long clientId) {
        String sql = """
                SELECT cl.client_name,
                       c.billing_cycle,
                       c.start_date
                FROM tbl_client cl
                LEFT JOIN tbl_contract_header c
                       ON c.client_id = cl.client_id
                      AND c.status NOT IN ('DELETED','EXPIRED','REJECTED_ADMIN','REJECTED_CLIENT','CANCELED')
                WHERE cl.client_id = :clientId
                ORDER BY c.start_date DESC
                LIMIT 1
                """;
        List<Map<String, Object>> results = jdbc.queryForList(sql,
                new MapSqlParameterSource().addValue("clientId", clientId));
        return results.stream()
                .findFirst()
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "Client not found: " + clientId));
    }

    // ──────────────────────────────────────────────
    // 2. 최근 주문 내역 (최근 5건)
    // ──────────────────────────────────────────────

    public List<Map<String, Object>> recentOrders(Long clientId) {
        String sql = """
                SELECT oh.order_code,
                       oh.created_at,
                       oh.status,
                       oh.total_amount,
                       -- 품목 요약: 첫 번째 품목명 + 나머지 건수
                       (SELECT CONCAT(
                                   MIN(od2.delivery_request),
                                   CASE WHEN COUNT(*) > 1
                                        THEN CONCAT(' 외 ', COUNT(*) - 1, '건')
                                        ELSE '' END)
                        FROM tbl_order_detail od2
                        WHERE od2.order_id = oh.order_id) AS summary
                FROM tbl_order_header oh
                WHERE oh.client_id = :clientId
                ORDER BY oh.created_at DESC
                LIMIT 5
                """;
        return jdbc.queryForList(sql,
                new MapSqlParameterSource().addValue("clientId", clientId));
    }

    // ──────────────────────────────────────────────
    // 3. 미결제·최근 청구서 (DRAFT/PUBLISHED, 최근 5건)
    // ──────────────────────────────────────────────

    public List<Map<String, Object>> unpaidInvoices(Long clientId) {
        String sql = """
                SELECT i.invoice_code,
                       i.invoice_date,
                       i.total_amount,
                       i.status
                FROM tbl_invoice i
                WHERE i.client_id = :clientId
                  AND i.status IN ('DRAFT', 'PUBLISHED')
                ORDER BY i.invoice_date ASC
                LIMIT 5
                """;
        return jdbc.queryForList(sql,
                new MapSqlParameterSource().addValue("clientId", clientId));
    }

    // ──────────────────────────────────────────────
    // 4. 최근 알림 (10건)
    // ──────────────────────────────────────────────

    public List<Map<String, Object>> recentNotifications(Long userId) {
        String sql = """
                SELECT n.title,
                       n.content,
                       n.created_at,
                       n.read_at
                FROM tbl_notification n
                WHERE n.user_id = :userId
                ORDER BY n.created_at DESC
                LIMIT 10
                """;
        return jdbc.queryForList(sql,
                new MapSqlParameterSource().addValue("userId", userId));
    }
}