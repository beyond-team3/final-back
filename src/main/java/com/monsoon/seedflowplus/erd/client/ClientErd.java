package com.monsoon.seedflowplus.erd.client;

import com.monsoon.seedflowplus.erd.account.EmployeeErd;
import com.monsoon.seedflowplus.erd.account.UserErd;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbl_client")
public class ClientErd {

    @Id
    @Column(name = "client_id")
    private Long clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_key", nullable = false)
    private UserErd account;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "brn", nullable = false, length = 10)
    private String brn;

    @Column(name = "ceo")
    private String ceo;

    @Column(name = "tel", nullable = false)
    private String tel;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "client_score")
    private Double clientScore;

    @Column(name = "manager", nullable = false)
    private String manager;

    @Column(name = "manager_email", nullable = false)
    private String managerEmail;

    @Column(name = "manager_tel", nullable = false)
    private String managerTel;

    @Column(name = "loan")
    private Long loan;

    @Column(name = "limit_loan", nullable = false)
    private Long limitLoan;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeErd employee;
}
