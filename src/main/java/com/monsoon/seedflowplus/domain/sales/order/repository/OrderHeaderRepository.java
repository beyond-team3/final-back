package com.monsoon.seedflowplus.domain.sales.order.repository;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface OrderHeaderRepository extends JpaRepository<OrderHeader, Long> {

    // 거래처 ID로 주문 목록 조회
    List<OrderHeader> findByClient_Id(Long clientId);

    // 계약 ID로 주문 목록 조회 (수량 검증 시 사용)
    List<OrderHeader> findByContract_Id(Long contractId);

    // 계약 ID + 취소 제외한 유효 주문만 조회
    List<OrderHeader> findByContract_IdAndStatusNot(Long contractId, OrderStatus status);

    // 주문 코드 중복 방지
    boolean existsByOrderCode(String orderCode);

    long countByOrderCodeStartingWith(String prefix);


    // 모든 주문 존재 거래처 ID 집합 조회
    @Query("SELECT DISTINCT o.client.id FROM OrderHeader o")
    Set<Long> findAllClientIdsWithOrders();

    // 거래처 존재 여부 확인 (스코어링용)
    boolean existsByClient(Client client);
}
