package com.monsoon.seedflowplus.domain.product.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductStatus {

    SALE("판매중"),
    STOP("판매중단"),
    SOLDOUT("품절"),
    HIDDEN("숨김");

    private final String description;
}