package com.monsoon.seedflowplus.domain.account.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@AttributeOverride(name = "id", column = @Column(name = "client_id"))
@Table(name = "tbl_client")
public class Client extends BaseModifyEntity {

    @Column(name = "client_code", nullable = false, unique = true)
    private String clientCode;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "client_brn", nullable = false, unique = true)
    private String clientBrn;

    @Column(name = "ceo_name", nullable = false)
    private String ceoName;

    @Column(name = "company_phone", nullable = false)
    private String companyPhone;

    @Column(name = "address", nullable = false)
    private String address;
    @Column(name = "latitude")
    private Double latitude; // 위도
    @Column(name = "longitude")
    private Double longitude; // 경도

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false)
    private ClientType clientType;

    @Column(name = "manager_name", nullable = false)
    private String managerName;
    @Column(name = "manager_phone", nullable = false)
    private String managerPhone;
    @Column(name = "manager_email", nullable = false)
    private String managerEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_code", referencedColumnName = "employee_code")
    private Employee managerEmployee;

    @Column(name = "total_credit", precision = 15, scale = 2)
    private BigDecimal totalCredit; // 전체 여신

    @Column(name = "used_credit", precision = 15, scale = 2)
    private BigDecimal usedCredit; // 사용 여신

    @OneToOne(mappedBy = "client")
    private User account;

    @Builder.Default
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClientCrop> crops = new ArrayList<>();

}
