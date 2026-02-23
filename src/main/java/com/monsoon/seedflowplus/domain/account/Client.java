package com.monsoon.seedflowplus.domain.account;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "tbl_client")
public class Client extends BaseModifyEntity {

    @Column(nullable = false, unique = true)
    private String clientCode;

    private String clientName;

    private String clientBrn;

    private String ceoName;

    private String companyPhone;

    private String address;

    private Double latitude;   // 위도
    private Double longitude;  // 경도

    private ClientType clientType;

    private String managerName;
    private String managerPhone;
    private String managerEmail;

    private Long employeeCode;

    @Column(name = "account_key")
    private Long accountId;

    @Builder.Default
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClientCrop> crops = new ArrayList<>();

}
