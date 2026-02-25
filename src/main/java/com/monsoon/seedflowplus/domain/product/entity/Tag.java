package com.monsoon.seedflowplus.domain.product.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_tag",
        // 카테고리 코드와 태그 이름의 조합은 무조건 유일해야 함
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tag_category_name",
                        columnNames = {"category_code", "tag_name"}
                )
        }
       )
@AttributeOverride(name = "id", column = @Column(name = "tag_id"))
public class Tag extends BaseCreateEntity {

    @Column(nullable = false, length = 30)
    private String categoryCode; // 태그 분류(상품 카테고리 X)

    @Column(nullable = false, length = 50)
    private String tagName;

    @Builder
    public Tag(String categoryCode, String tagName) {
        this.categoryCode = categoryCode;
        this.tagName = tagName;
    }
}