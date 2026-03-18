package com.monsoon.seedflowplus.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.product.entity.Tag;
import com.monsoon.seedflowplus.domain.product.repository.TagRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TagReadServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagReadService tagReadService;

    @Test
    @DisplayName("전체 태그를 카테고리별로 그룹화하여 반환한다")
    void getAllTagsGroupedByCategory_Success() {
        // given
        Tag tag1 = Tag.builder().categoryCode("재배환경").tagName("노지").build();
        Tag tag2 = Tag.builder().categoryCode("재배환경").tagName("하우스").build();
        Tag tag3 = Tag.builder().categoryCode("내병성").tagName("탄저병").build();
        ReflectionTestUtils.setField(tag1, "id", 1L);
        ReflectionTestUtils.setField(tag2, "id", 2L);
        ReflectionTestUtils.setField(tag3, "id", 3L);

        when(tagRepository.findAll()).thenReturn(List.of(tag1, tag2, tag3));

        // when
        Map<String, List<String>> result = tagReadService.getAllTagsGroupedByCategory();

        // then
        assertThat(result).containsKey("재배환경");
        assertThat(result).containsKey("내병성");
        assertThat(result.get("재배환경")).containsExactlyInAnyOrder("노지", "하우스");
        assertThat(result.get("내병성")).containsExactly("탄저병");
    }

    @Test
    @DisplayName("태그가 없으면 빈 Map을 반환한다")
    void getAllTagsGroupedByCategory_Empty() {
        // given
        when(tagRepository.findAll()).thenReturn(List.of());

        // when
        Map<String, List<String>> result = tagReadService.getAllTagsGroupedByCategory();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("동일한 카테고리에 여러 태그가 있을 때 모두 같은 그룹에 포함된다")
    void getAllTagsGroupedByCategory_MultipleTagsSameCategory() {
        // given
        Tag tag1 = Tag.builder().categoryCode("과실품질").tagName("당도높음").build();
        Tag tag2 = Tag.builder().categoryCode("과실품질").tagName("식감좋음").build();
        Tag tag3 = Tag.builder().categoryCode("과실품질").tagName("색상균일").build();
        ReflectionTestUtils.setField(tag1, "id", 1L);
        ReflectionTestUtils.setField(tag2, "id", 2L);
        ReflectionTestUtils.setField(tag3, "id", 3L);

        when(tagRepository.findAll()).thenReturn(List.of(tag1, tag2, tag3));

        // when
        Map<String, List<String>> result = tagReadService.getAllTagsGroupedByCategory();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get("과실품질")).hasSize(3);
        assertThat(result.get("과실품질")).containsExactlyInAnyOrder("당도높음", "식감좋음", "색상균일");
    }
}
