package com.monsoon.seedflowplus.domain.product.repository;

import com.monsoon.seedflowplus.domain.product.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    // 카테고리, 태크이름으로 태그 존재여부 확인
    Optional<Tag> findByCategoryCodeAndTagName(String categoryCode, String tagName);
}
