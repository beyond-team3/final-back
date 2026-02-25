package com.monsoon.seedflowplus.domain.account.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "id", column = @Column(name = "client_crop_id"))
@Table(name = "tbl_client_crops")
public class ClientCrop extends BaseEntity {

    @Column(nullable = false)
    private String cropName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Builder
    public ClientCrop(String cropName, Client client) {
        this.cropName = cropName;
        this.client = client;
    }
}
