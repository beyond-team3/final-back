package com.monsoon.seedflowplus.domain.account.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
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
    @JoinColumn(name = "employee_id")
    private Employee managerEmployee;

    @Column(name = "total_credit", precision = 15, scale = 2)
    private BigDecimal totalCredit; // 전체 여신

    @Column(name = "used_credit", precision = 15, scale = 2)
    private BigDecimal usedCredit; // 사용 여신

    @OneToOne(mappedBy = "client")
    private User account;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClientCrop> crops = new ArrayList<>();

    @Builder
    public Client(String clientCode, String clientName, String clientBrn, String ceoName,
                    String companyPhone, String address, Double latitude, Double longitude,
                    ClientType clientType, String managerName, String managerPhone,
                    String managerEmail, Employee managerEmployee, BigDecimal totalCredit,
                    BigDecimal usedCredit) {
        this.clientCode = clientCode;
        this.clientName = clientName;
        this.clientBrn = clientBrn;
        this.ceoName = ceoName;
        this.companyPhone = companyPhone;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.clientType = clientType;
        this.managerName = managerName;
        this.managerPhone = managerPhone;
        this.managerEmail = managerEmail;
        this.managerEmployee = managerEmployee;
        this.totalCredit = totalCredit != null ? totalCredit : BigDecimal.ZERO;
        this.usedCredit = usedCredit != null ? usedCredit : BigDecimal.ZERO;
    }

    public void updateClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    public void updateManagerEmployee(Employee employee) {
        this.managerEmployee = employee;
    }

    public void updateClientInfo(String clientName, String clientBrn, String ceoName,
                                String companyPhone, String address, ClientType clientType,
                                String managerName, String managerPhone, String managerEmail,
                                BigDecimal totalCredit) {
        if (clientName != null)
            this.clientName = clientName;
        if (clientBrn != null)
            this.clientBrn = clientBrn;
        if (ceoName != null)
            this.ceoName = ceoName;
        if (companyPhone != null)
            this.companyPhone = companyPhone;
        if (address != null)
            this.address = address;
        if (clientType != null)
            this.clientType = clientType;
        if (managerName != null)
            this.managerName = managerName;
        if (managerPhone != null)
            this.managerPhone = managerPhone;
        if (managerEmail != null)
            this.managerEmail = managerEmail;
        if (totalCredit != null)
            this.totalCredit = totalCredit;
    }

}
