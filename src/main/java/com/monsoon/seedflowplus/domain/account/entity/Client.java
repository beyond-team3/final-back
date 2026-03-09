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

    public void updateCoordinates(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
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
        if (clientName != null && !clientName.isBlank())
            this.clientName = clientName.trim();
        if (clientBrn != null && !clientBrn.isBlank())
            this.clientBrn = clientBrn.trim();
        if (ceoName != null && !ceoName.isBlank())
            this.ceoName = ceoName.trim();
        if (companyPhone != null && !companyPhone.isBlank())
            this.companyPhone = companyPhone.trim();
        if (address != null && !address.isBlank())
            this.address = address.trim();
        if (clientType != null)
            this.clientType = clientType;
        if (managerName != null && !managerName.isBlank())
            this.managerName = managerName.trim();
        if (managerPhone != null && !managerPhone.isBlank())
            this.managerPhone = managerPhone.trim();
        if (managerEmail != null && !managerEmail.isBlank())
            this.managerEmail = managerEmail.trim();
        if (totalCredit != null) {
            if (this.usedCredit != null && totalCredit.compareTo(this.usedCredit) < 0) {
                throw new IllegalArgumentException("총 크레딧은 사용한 크레딧보다 적을 수 없습니다.");
            }
            this.totalCredit = totalCredit;
        }
    }

}
