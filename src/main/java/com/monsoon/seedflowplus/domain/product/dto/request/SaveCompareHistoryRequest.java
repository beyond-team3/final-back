package com.monsoon.seedflowplus.domain.product.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SaveCompareHistoryRequest {

    @NotNull(message = "비교할 상품 ID 목록은 필수입니다.")
    private List<Long> productIds;

    private String title;
}
