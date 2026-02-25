package com.monsoon.seedflowplus.domain.document.order.repository;

import com.monsoon.seedflowplus.domain.document.order.entity.OrderHeader;
import com.monsoon.seedflowplus.domain.document.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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
}
