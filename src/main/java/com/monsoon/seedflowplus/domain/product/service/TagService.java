package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.product.entity.Tag;
import com.monsoon.seedflowplus.domain.product.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public Tag getOrCreateTag(String categoryCode, String inputTagName) {
        String normalized = normalizeTagName(inputTagName);

        if (normalized.isEmpty()) {
            return null; // 빈 값이면 그냥 null 반환 (무시)
        }

        // 있으면 가져오고, 없으면 새로 만들어서 저장 후 반환
        return tagRepository.findByCategoryCodeAndTagName(categoryCode, normalized)
                .orElseGet(() -> tagRepository.save(
                        Tag.builder()
                                .categoryCode(categoryCode)
                                .tagName(normalized)
                                .build()
                ));
    }
}
