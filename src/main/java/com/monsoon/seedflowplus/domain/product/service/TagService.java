package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.product.entity.Tag;
import com.monsoon.seedflowplus.domain.product.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    // 공백 제거
    private String normalizeTagName(String tagName) {
        return tagName == null ? "" : tagName.replaceAll("\\s+", "");
    }


    // 단건 태그 생성 (관리자 화면용 - 에러 발생시킴)
    @Transactional
    public void createNewTag(String categoryCode, String inputTagName) {
        if (categoryCode == null || inputTagName == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        String normalized = normalizeTagName(inputTagName);

        if (normalized.isEmpty()) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (tagRepository.existsByCategoryCodeAndTagName(categoryCode, normalized)) {
            throw new CoreException(ErrorType.DUPLICATE_TAG);
        }

        tagRepository.save(Tag.builder()
                .categoryCode(categoryCode)
                .tagName(normalized)
                .build());
    }

    // 태그 조회 또는 생성 (상품 등록/수정 시 사용)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Tag getOrCreateTag(String categoryCode, String inputTagName) {
        String normalized = normalizeTagName(inputTagName);

        if (normalized.isEmpty()) {
            return null; // 빈 값이면 그냥 null 반환 (무시)
        }

        // 중복 여부 확인
        Optional<Tag> existingTag = tagRepository.findByCategoryCodeAndTagName(categoryCode, normalized);
        if (existingTag.isPresent()) {
            return existingTag.get();
        }

        // 없으면 새로 저장 시도
        try {
            Tag newTag = Tag.builder()
                    .categoryCode(categoryCode)
                    .tagName(normalized)
                    .build();

            // 강제 중복 검사
            return tagRepository.saveAndFlush(newTag);

        } catch (DataIntegrityViolationException e) {
            /* 동시에 다른 사람이 먼저 똑같은 태그를 만들어서 유니크 에러발생시
            다른 사람이 방금 만든 그 태그를 조회해서 반환 */
            return tagRepository.findByCategoryCodeAndTagName(categoryCode, normalized)
                    .orElseThrow(() -> new CoreException(ErrorType.DEFAULT_ERROR));
        }
    }
}
