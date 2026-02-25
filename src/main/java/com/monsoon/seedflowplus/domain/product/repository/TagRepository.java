package com.monsoon.seedflowplus.domain.product.repository;

import com.monsoon.seedflowplus.domain.product.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    // 카테고리, 태크이름으로 태그 존재여부 확인 (객체 상태)
    Optional<Tag> findByCategoryCodeAndTagName(String categoryCode, String tagName);

    // 태그가 존재하는지 여부만 빠르게 확인 (boolean 반환)
    boolean existsByCategoryCodeAndTagName(String categoryCode, String tagName);
}
