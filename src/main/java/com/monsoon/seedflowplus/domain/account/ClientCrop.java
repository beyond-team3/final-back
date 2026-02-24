package com.monsoon.seedflowplus.domain.account;

import com.monsoon.seedflowplus.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverride(name = "id", column = @Column(name = "client_crop_id"))
@Table(name = "tbl_client_crops")
public class ClientCrop extends BaseEntity {

    @Column(nullable = false)
    private String cropName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;
}
