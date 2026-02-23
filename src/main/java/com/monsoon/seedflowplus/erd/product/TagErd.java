package com.monsoon.seedflowplus.erd.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_tag")
public class TagErd extends BaseCreateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tagId; // 태그pk

    @Column(nullable = false, length = 30)
    private String categoryCode; // 태그 분류

    @Column(nullable = false, length = 50)
    private String tagName; // 태그 명

    @Builder
    public TagErd(String categoryCode, String tagName) {
        this.categoryCode = categoryCode;
        this.tagName = tagName;
    }
}