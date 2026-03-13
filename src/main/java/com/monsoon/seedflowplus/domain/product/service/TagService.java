package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.product.entity.Tag;
import com.monsoon.seedflowplus.domain.product.repository.TagRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final EntityManager entityManager;

    // 공백 제거
    private String normalizeTagName(String tagName) {
        return tagName == null ? "" : tagName.replaceAll("\\s+", "");
    }


    // 단건 태그 생성 (관리자 화면용)
    @Transactional
    public void createNewTag(String categoryCode, String inputTagName) {

        if (categoryCode == null || categoryCode.isBlank() || inputTagName == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        String normalized = normalizeTagName(inputTagName);

        if (normalized.isEmpty()) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        try {
             tagRepository.saveAndFlush(Tag.builder()
                            .categoryCode(categoryCode)
                            .tagName(normalized)
                            .build());
        } catch (DataIntegrityViolationException e) {
            throw new CoreException(ErrorType.DUPLICATE_TAG);
        }
    }

    // 태그 조회 또는 생성 (상품 등록/수정 시 사용)
    // 외부 트랜잭션(createProduct/updateProduct)에 참여하여 snapshot visibility 충돌 방지
    @Transactional
    public Tag getOrCreateTag(String categoryCode, String inputTagName) {

        if (categoryCode == null || categoryCode.isBlank()) {
            return null;
        }

        String normalized = normalizeTagName(inputTagName);

        if (normalized.isEmpty()) {
            return null; // 빈 값이면 생성 X
        }

        // 중복 여부 확인
        Optional<Tag> existingTag = tagRepository.findByCategoryCodeAndTagName(categoryCode, normalized);
        if (existingTag.isPresent()) {
            return existingTag.get();
        }

        // 없으면 외부 트랜잭션 안에서 저장 및 즉시 flush하여 제약 조건 위반을 이 메서드 내에서 처리
        Tag newTag = Tag.builder()
                .categoryCode(categoryCode)
                .tagName(normalized)
                .build();
        try {
            return tagRepository.saveAndFlush(newTag);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 같은 태그가 먼저 생성된 경우:
            // 실패한 엔티티를 영속성 컨텍스트에서 제거하여 외부 트랜잭션 오염 방지
            entityManager.detach(newTag);
            return tagRepository.findByCategoryCodeAndTagName(categoryCode, normalized)
                    .orElseThrow(() -> new CoreException(ErrorType.DEFAULT_ERROR));
        }
    }
}
