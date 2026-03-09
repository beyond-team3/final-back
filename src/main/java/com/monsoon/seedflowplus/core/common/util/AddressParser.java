package com.monsoon.seedflowplus.core.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AddressParser {

    public static AddressInfo parse(String address) {
        if (address == null || address.isBlank()) {
            return new AddressInfo("", "", "");
        }

        String[] parts = address.split("/", -1);

        if (parts.length >= 3) {
            return new AddressInfo(parts[0].trim(), parts[1].trim(), parts[2].trim());
        } else if (parts.length == 2) {
            return new AddressInfo(parts[0].trim(), parts[1].trim(), "");
        } else {
            // 슬래시가 없는 경우, sido와 detail 모두에 전체 주소를 저장하여 모든 뷰에서 보이도록 함
            String trimmedAddress = address.trim();
            return new AddressInfo(trimmedAddress, trimmedAddress, "");
        }
    }

    public record AddressInfo(String sido, String detail, String zip) {
    }
}
