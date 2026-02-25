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

    @Transactional
    public void createNewTag(String categoryCode, String inputTagName) {

        String normalizedTagName = inputTagName.replaceAll("\\s+", "");

        // 빈값
        if (normalizedTagName.isEmpty()) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        // 태그 중복여부 확인
        boolean isExist = tagRepository.existsByCategoryCodeAndTagName(categoryCode, normalizedTagName);

        // 중복 예외
        if (isExist) {
            throw new CoreException(ErrorType.DUPLICATE_TAG);
        }

        // 존재 여부 확인후 저장
        Tag newTag = Tag.builder()
                .categoryCode(categoryCode)
                .tagName(normalizedTagName)
                .build();

        tagRepository.save(newTag);
    }
}
