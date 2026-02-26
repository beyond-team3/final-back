package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.ClientCrop;

public record ClientCropResponse(
        Long id,
        String cropName) {
    public static ClientCropResponse from(ClientCrop clientCrop) {
        return new ClientCropResponse(
                clientCrop.getId(),
                clientCrop.getCropName());
    }
}
