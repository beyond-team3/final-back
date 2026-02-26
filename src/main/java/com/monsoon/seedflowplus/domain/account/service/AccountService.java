package com.monsoon.seedflowplus.domain.account.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.dto.request.ClientRegisterRequest;
import com.monsoon.seedflowplus.domain.account.dto.request.EmployeeRegisterRequest;
import com.monsoon.seedflowplus.domain.account.dto.request.UserCreateRequest;
import com.monsoon.seedflowplus.domain.account.dto.request.UserStatusUpdateRequest;
import com.monsoon.seedflowplus.domain.account.entity.*;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private  final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerClient(ClientRegisterRequest request) {

        // 1. 중복 검사
        if(clientRepository.existsByClientBrn(request.clientBrn())) {
            throw new CoreException(ErrorType.DUPLICATE_CLIENT_BRN);
        }

        // 2. 거래처 정보 등록 및 저장
        // clientCode는 cli-유형-pk 형식이므로, 먼저 임시값으로 저장 후 PK를 획득하여 업데이트함
        String tempCode = "TEMP-" + UUID.randomUUID();

        Client client = Client.builder()
                .clientCode(tempCode)
                .clientName(request.clientName())
                .clientBrn(request.clientBrn())
                .ceoName(request.ceoName())
                .companyPhone(request.companyPhone())
                .address(request.address())
                .clientType(request.clientType())
                .managerName(request.managerName())
                .managerPhone(request.managerPhone())
                .managerEmail(request.managerEmail())
                .totalCredit(request.totalCredit())
                .build();

        clientRepository.save(client);

        // PK(client_id)를 포함한 최종 clientCode 생성 및 업데이트 (4자리 제로 패딩)
        String finalClientCode = String.format("CLNT-%04d", client.getId());
        client.updateClientCode(finalClientCode);

    }

    @Transactional
    public void registerEmployee(EmployeeRegisterRequest request) {

        // 1. Employee 생성 및 저장
        // employeeCode는 EMP-000x 형식이므로, 먼저 임시값으로 저장 후 PK를 획득하여 업데이트함
        String tempCode = "TEMP-" + UUID.randomUUID();

        Employee employee = Employee.builder()
                .employeeCode(tempCode)
                .employeeName(request.employeeName())
                .employeeEmail(request.employeeEmail())
                .employeePhone(request.employeePhone())
                .address(request.address())
                .build();

        employeeRepository.save(employee);

        // PK(employee_id)를 포함한 최종 employeeCode 생성 및 업데이트 (4자리 제로 패딩)
        String finalEmployeeCode = String.format("EMP-%04d", employee.getId());
        employee.updateEmployeeCode(finalEmployeeCode);

    }

    @Transactional
    public void createAccount(UserCreateRequest request) {
        // 1. 중복 ID 체크
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new CoreException(ErrorType.DUPLICATE_LOGIN_ID);
        }

        // 2. User 객체 빌더 준비
        User.UserBuilder userBuilder = User.builder()
                .loginId(request.loginId())
                .loginPw(passwordEncoder.encode(request.loginPw()))
                .status(Status.DEACTIVATE) // 기본 비활성화
                .role(request.role());

        // 3. 권한별 연동 처리
        if (request.role() == Role.CLIENT) {
            Client client = clientRepository.findById(request.targetId())
                    .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));

            // 담당 영업사원 업데이트 (요청에 포함된 경우)
            if (request.managerEmployeeId() != null) {
                Employee manager = employeeRepository.findById(request.managerEmployeeId())
                        .orElseThrow(() -> new CoreException(ErrorType.EMPLOYEE_NOT_FOUND));
                client.updateManagerEmployee(manager);
            }

            userBuilder.client(client);
        } else {
            // ADMIN, SALES_REP 등은 Employee와 연동
            Employee employee = employeeRepository.findById(request.targetId())
                    .orElseThrow(() -> new CoreException(ErrorType.EMPLOYEE_NOT_FOUND));
            userBuilder.employee(employee);
        }

        userRepository.save(userBuilder.build());
    }

    @Transactional
    public void updateUserStatus(UserStatusUpdateRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        user.updateStatus(request.status());
    }

}
