package com.monsoon.seedflowplus.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.product.entity.Tag;
import com.monsoon.seedflowplus.domain.product.repository.TagRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TagService tagService;

    // ─── createNewTag ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("유효한 카테고리와 태그명으로 태그 생성 시 저장된다")
    void createNewTag_Success() {
        // given
        String categoryCode = "재배환경";
        String tagName = "노지";
        Tag saved = Tag.builder().categoryCode(categoryCode).tagName(tagName).build();
        when(tagRepository.saveAndFlush(any(Tag.class))).thenReturn(saved);

        // when
        tagService.createNewTag(categoryCode, tagName);

        // then
        verify(tagRepository, times(1)).saveAndFlush(any(Tag.class));
    }

    @Test
    @DisplayName("유효하지 않은 카테고리 코드로 태그 생성 시 INVALID_INPUT_VALUE 예외가 발생한다")
    void createNewTag_InvalidCategory() {
        assertThatThrownBy(() -> tagService.createNewTag("잘못된카테고리", "노지"))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("태그명이 null이면 INVALID_INPUT_VALUE 예외가 발생한다")
    void createNewTag_NullTagName() {
        assertThatThrownBy(() -> tagService.createNewTag("재배환경", null))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("태그명이 공백만이면 INVALID_INPUT_VALUE 예외가 발생한다")
    void createNewTag_BlankTagName() {
        assertThatThrownBy(() -> tagService.createNewTag("재배환경", "   "))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("이미 존재하는 태그를 생성하면 DUPLICATE_TAG 예외가 발생한다")
    void createNewTag_DuplicateTag() {
        when(tagRepository.saveAndFlush(any(Tag.class)))
                .thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> tagService.createNewTag("재배환경", "노지"))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.DUPLICATE_TAG);
    }

    // ─── getOrCreateTag ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("기존에 존재하는 태그 조회 시 기존 태그를 반환한다")
    void getOrCreateTag_ExistingTag() {
        // given
        String categoryCode = "내병성";
        String tagName = "탄저병";
        Tag existingTag = Tag.builder().categoryCode(categoryCode).tagName(tagName).build();
        ReflectionTestUtils.setField(existingTag, "id", 1L);

        when(tagRepository.findByCategoryCodeAndTagName(categoryCode, tagName))
                .thenReturn(Optional.of(existingTag));

        // when
        Tag result = tagService.getOrCreateTag(categoryCode, tagName);

        // then
        assertThat(result).isEqualTo(existingTag);
        verify(tagRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("존재하지 않는 태그 조회 시 새 태그를 생성하여 반환한다")
    void getOrCreateTag_NewTag() {
        // given
        String categoryCode = "생육및숙기";
        String tagName = "중생";
        Tag newTag = Tag.builder().categoryCode(categoryCode).tagName(tagName).build();

        when(tagRepository.findByCategoryCodeAndTagName(categoryCode, tagName)).thenReturn(Optional.empty());
        when(tagRepository.saveAndFlush(any(Tag.class))).thenReturn(newTag);

        // when
        Tag result = tagService.getOrCreateTag(categoryCode, tagName);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTagName()).isEqualTo(tagName);
    }

    @Test
    @DisplayName("태그명에 공백이 포함되어 있으면 공백을 제거하여 처리한다")
    void getOrCreateTag_NormalizesWhitespace() {
        // given
        String categoryCode = "과실품질";
        String tagName = "당도 높음"; // 공백 포함
        String normalized = "당도높음";
        Tag newTag = Tag.builder().categoryCode(categoryCode).tagName(normalized).build();

        when(tagRepository.findByCategoryCodeAndTagName(categoryCode, normalized)).thenReturn(Optional.empty());
        when(tagRepository.saveAndFlush(any(Tag.class))).thenReturn(newTag);

        // when
        Tag result = tagService.getOrCreateTag(categoryCode, tagName);

        // then
        assertThat(result.getTagName()).isEqualTo(normalized);
    }

    @Test
    @DisplayName("카테고리 코드가 null이면 null을 반환한다")
    void getOrCreateTag_NullCategory_ReturnsNull() {
        Tag result = tagService.getOrCreateTag(null, "노지");
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("태그명이 빈 문자열이면 null을 반환한다")
    void getOrCreateTag_EmptyTagName_ReturnsNull() {
        Tag result = tagService.getOrCreateTag("재배환경", "  ");
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("유효하지 않은 카테고리 코드로 getOrCreateTag 호출 시 INVALID_INPUT_VALUE 예외가 발생한다")
    void getOrCreateTag_InvalidCategory_ThrowsException() {
        assertThatThrownBy(() -> tagService.getOrCreateTag("잘못된카테고리", "노지"))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("동시 요청으로 중복 생성 충돌 시 기존 태그를 반환한다")
    void getOrCreateTag_ConcurrentDuplicate_ReturnExisting() {
        // given
        String categoryCode = "재배편의성";
        String tagName = "관리쉬움";
        Tag existingTag = Tag.builder().categoryCode(categoryCode).tagName(tagName).build();

        when(tagRepository.findByCategoryCodeAndTagName(categoryCode, tagName))
                .thenReturn(Optional.empty())        // 첫 번째 조회: 없음
                .thenReturn(Optional.of(existingTag)); // 재조회: 존재
        when(tagRepository.saveAndFlush(any(Tag.class)))
                .thenThrow(DataIntegrityViolationException.class); // 저장 실패 (동시 생성)
        doNothing().when(entityManager).detach(any());

        // when
        Tag result = tagService.getOrCreateTag(categoryCode, tagName);

        // then
        assertThat(result).isEqualTo(existingTag);
    }
}
