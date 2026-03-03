package com.monsoon.seedflowplus.domain.product.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductCategory {

    // 과채류 (열매채소)
    WATERMELON("수박", "WM"),
    PEPPER("고추", "PP"),
    TOMATO("토마토", "TM"),
    CUCUMBER("오이", "CU"),
    MELON("참외", "ML"),

    // 엽근채류 (잎/뿌리채소)
    CABBAGE("배추", "CB"),
    RADISH("무", "RD"),
    ONION("양파", "ON"),
    GREEN_ONION("파", "GO"),

    // 식량작물 기타
    CORN("옥수수", "CN"),
    BEAN("콩", "BN");

    // 속성
    private final String description; // 화면 노출
    private final String code;        // 상품 코드 생성용 약자
}
