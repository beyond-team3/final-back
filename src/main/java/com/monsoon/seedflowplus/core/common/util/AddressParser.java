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
            // 슬래시가 없거나 하나만 있는 경우 기존 주소로 간주하여 sido에 전체 저장
            return new AddressInfo(address.trim(), "", "");
        }
    }

    public record AddressInfo(String sido, String detail, String zip) {
    }
}
