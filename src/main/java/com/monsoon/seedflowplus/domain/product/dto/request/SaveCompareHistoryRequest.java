package com.monsoon.seedflowplus.domain.product.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SaveCompareHistoryRequest {

    @NotEmpty(message = "비교할 상품 ID 목록은 비어있을 수 없습니다.")
    private List<@NotNull(message = "상품 ID는 null일 수 없습니다.") Long> productIds;

    private String title;
}
