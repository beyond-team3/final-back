package com.monsoon.seedflowplus.domain.map.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PestMapSearchRequest {
    private String cropCode;
    private String pestCode;
}