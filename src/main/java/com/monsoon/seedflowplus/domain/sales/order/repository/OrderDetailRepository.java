package com.monsoon.seedflowplus.domain.sales.order.repository;

import com.monsoon.seedflowplus.domain.sales.order.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    // 주문 ID로 디테일 목록 조회
    List<OrderDetail> findByOrderHeader_Id(Long orderId);

    // 특정 계약 디테일(품목)에 대해 현재까지 주문된 총 수량 합산
    // - 취소된 주문 제외
    // - Service에서 잔여 수량 검증 시 사용
    @Query("""
        SELECT COALESCE(SUM(od.quantity), 0)
        FROM OrderDetail od
        JOIN od.orderHeader oh
        WHERE od.contractDetail.id = :contractDetailId
          AND oh.status <> 'CANCELED'
    """)
    Long sumQuantityByContractDetailId(@Param("contractDetailId") Long contractDetailId);
}