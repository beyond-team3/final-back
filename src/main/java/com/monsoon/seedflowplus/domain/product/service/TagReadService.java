package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.domain.product.entity.Tag;
import com.monsoon.seedflowplus.domain.product.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagReadService {

    private final TagRepository tagRepository;

    /* 포론트엔드 태그 토글 버튼을 위한 조회 */
    public Map<String, List<String>> getAllTagsGroupedByCategory() {

        List<Tag> allTags = tagRepository.findAll();

        return allTags.stream()
                .collect(Collectors.groupingBy(
                        Tag::getCategoryCode, // Key: 카테고리 코드
                        Collectors.mapping(Tag::getTagName, Collectors.toList()) // Value: 태그 리스트
                ));
    }
}
