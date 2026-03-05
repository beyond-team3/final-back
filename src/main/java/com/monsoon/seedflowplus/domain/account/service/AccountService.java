package com.monsoon.seedflowplus.domain.account.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.dto.request.*;
import com.monsoon.seedflowplus.domain.account.dto.response.AssignedEmployeeResponse;
import com.monsoon.seedflowplus.domain.account.dto.response.ClientCropResponse;
import com.monsoon.seedflowplus.domain.account.dto.response.ClientDetailResponse;
import com.monsoon.seedflowplus.domain.account.dto.response.ClientListForDocumentResponse;
import com.monsoon.seedflowplus.domain.account.dto.response.ClientListResponse;
import com.monsoon.seedflowplus.domain.account.dto.response.ClientProfileResponse;
import com.monsoon.seedflowplus.domain.account.dto.response.EmployeeDetailResponse;
import com.monsoon.seedflowplus.domain.account.dto.response.EmployeeListResponse;
import com.monsoon.seedflowplus.domain.account.dto.response.EmployeeManagedClientResponse;
import com.monsoon.seedflowplus.domain.account.dto.response.EmployeeSimpleResponse;
import com.monsoon.seedflowplus.domain.account.dto.response.UnregisteredClientResponse;
import com.monsoon.seedflowplus.domain.account.dto.response.UnregisteredEmployeeResponse;
import com.monsoon.seedflowplus.domain.account.entity.*;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.account.repository.ClientCropRepository;
import com.monsoon.seedflowplus.infra.kakao.GeocodingService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final ClientCropRepository clientCropRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTokenStore tokenStore;
    private final GeocodingService geocodingService;

    @Transactional
    public ClientDetailResponse registerClient(ClientRegisterRequest request) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        requireRole(userDetails, Role.ADMIN);

        // 1. 중복 검사
        if (clientRepository.existsByClientBrn(request.clientBrn())) {
            throw new CoreException(ErrorType.DUPLICATE_CLIENT_BRN);
        }

        // 2. 거래처 정보 등록 및 저장
        // clientCode는 cli-유형-pk 형식이므로, 먼저 임시값으로 저장 후 PK를 획득하여 업데이트함
        String tempCode = "TCLNT-" + UUID.randomUUID();

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

        // 좌표 정보 추가 (Geocoding) - 서비스 코드 수정 최소화
        geocodingService.fillCoordinates(client);

        clientRepository.save(client);

        // PK(client_id)를 포함한 최종 clientCode 생성 및 업데이트 (4자리 제로 패딩)
        String finalClientCode = String.format("CLNT-%04d", client.getId());
        client.updateClientCode(finalClientCode);

        // 저장된 결과를 반환하기 위해 조회 (UserDetails는 없으므로 client 엔티티로부터 수동 변환 혹은 DTO 활용)
        // ClientDetailResponse.from(client)가 User를 필요로 하는지 확인이 필요함
        return ClientDetailResponse.from(client);
    }

    @Transactional
    public void registerEmployee(EmployeeRegisterRequest request) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        requireRole(userDetails, Role.ADMIN);

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

    @Transactional
    public void updateClientInfo(Long clientId, ClientUpdateRequest request) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));

        // 사업자번호(BRN) 변경 시 중복 체크
        if (request.clientBrn() != null && !request.clientBrn().equals(client.getClientBrn())) {
            if (clientRepository.existsByClientBrnAndIdNot(request.clientBrn(), clientId)) {
                throw new CoreException(ErrorType.DUPLICATE_CLIENT_BRN);
            }
        }

        client.updateClientInfo(
                request.clientName(),
                request.clientBrn(),
                request.ceoName(),
                request.companyPhone(),
                request.address(),
                request.clientType(),
                request.managerName(),
                request.managerPhone(),
                request.managerEmail(),
                request.totalCredit());
    }

    @Transactional
    public void updateEmployeeInfo(Long employeeId, EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new CoreException(ErrorType.EMPLOYEE_NOT_FOUND));

        employee.updateEmployeeInfo(
                request.employeeName(),
                request.employeeEmail(),
                request.employeePhone(),
                request.address());
    }

    @Transactional
    public void addClientCrop(Long clientId, ClientCropRequest request) {
        Client client = validateAndGetClient(clientId);

        ClientCrop clientCrop = ClientCrop.builder()
                .cropName(request.cropName())
                .client(client)
                .build();

        clientCropRepository.save(clientCrop);
    }

    @Transactional(readOnly = true)
    public List<ClientCropResponse> getClientCrops(Long clientId) {
        validateAndGetClient(clientId);

        return clientCropRepository.findAllByClientId(clientId).stream()
                .map(ClientCropResponse::from)
                .toList();
    }

    @Transactional
    public void deleteClientCrop(Long cropId) {
        ClientCrop clientCrop = clientCropRepository.findById(cropId)
                .orElseThrow(() -> new CoreException(ErrorType.CROP_NOT_FOUND));

        validateAndGetClient(clientCrop.getClient().getId());

        clientCropRepository.delete(clientCrop);
    }

    private Client validateAndGetClient(Long clientId) {
        CustomUserDetails userDetails = getAuthenticatedUser();

        // 관리자는 클라이언트 정보 반환
        if (userDetails.getRole() == Role.ADMIN) {
            return clientRepository.findById(clientId)
                    .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));
        }

        if (userDetails.getRole() == Role.SALES_REP) {
            // 해당 거래처의 담당자인지 확인
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));

            if (client.getManagerEmployee() == null || userDetails.getEmployeeId() == null
                    || !client.getManagerEmployee().getId().equals(userDetails.getEmployeeId())) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return client;
        } else if (userDetails.getRole() == Role.CLIENT) {
            // 본인 거래처인지 확인
            if (userDetails.getClientId() == null || !userDetails.getClientId().equals(clientId)) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            // 세션에서 엔티티를 제거했으므로 DB에서 조회
            return clientRepository.findById(clientId)
                    .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));
        } else {
            // 그 외 역할은 접근 불가
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request) {
        CustomUserDetails userDetails = getAuthenticatedUser();

        // 보안을 위해 세션의 정보 대신 DB에서 최신 유저 정보를 조회
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.oldPassword(), user.getLoginPw())) {
            throw new CoreException(ErrorType.INVALID_PASSWORD);
        }

        if (passwordEncoder.matches(request.newPassword(), user.getLoginPw())) {
            throw new CoreException(ErrorType.SAME_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user); // Persistence Context에 의해 자동으로 반영되지만 명시적으로 호출할 수도 있음
        tokenStore.deleteRefreshToken(user.getLoginId());
    }

    @Transactional(readOnly = true)
    public List<EmployeeListResponse> getAllEmployees() {
        CustomUserDetails userDetails = getAuthenticatedUser();
        requireRole(userDetails, Role.ADMIN);

        return userRepository.findAllByEmployeeIsNotNull().stream()
                .map(EmployeeListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeDetailResponse getEmployeeDetail(Long employeeId) {
        CustomUserDetails userDetails = getAuthenticatedUser();

        // 권한 체크: ADMIN이 아니고, 본인의 employeeId와 요청된 employeeId가 다른 경우 거부
        if (userDetails.getRole() != Role.ADMIN &&
                (userDetails.getEmployeeId() == null || !userDetails.getEmployeeId().equals(employeeId))) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        return EmployeeDetailResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<ClientListForDocumentResponse> getClientsForDocument() {
        CustomUserDetails userDetails = getAuthenticatedUser();
        requireRole(userDetails, Role.SALES_REP);

        // 담당 사원 ID가 없는 경우 (이론상 SALES_REP이면 있어야 함)
        if (userDetails.getEmployeeId() == null) {
            throw new CoreException(ErrorType.EMPLOYEE_NOT_LINKED);
        }

        return clientRepository.findAllByManagerEmployeeId(userDetails.getEmployeeId()).stream()
                .map(ClientListForDocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientListForDocumentResponse getClientForDocument(Long clientId) {
        // 역할 체크를 먼저 수행 (SALES_REP 전용)
        CustomUserDetails userDetails = getAuthenticatedUser();
        requireRole(userDetails, Role.SALES_REP);

        // 권한 체크 및 거래처 조회 (validateAndGetClient 내부에서 담당자인지 확인)
        Client client = validateAndGetClient(clientId);

        return ClientListForDocumentResponse.from(client);
    }

    @Transactional(readOnly = true)
    public List<ClientListResponse> getAllClients() {
        CustomUserDetails userDetails = getAuthenticatedUser();

        Role role = userDetails.getRole();

        // 관리자인 경우 전체 조회
        if (role == Role.ADMIN) {
            return clientRepository.findAll().stream()
                    .map(ClientListResponse::from)
                    .toList();
        }

        // 영업사원인 경우 본인 담당 거래처만 조회
        if (role == Role.SALES_REP) {
            if (userDetails.getEmployeeId() == null) {
                throw new CoreException(ErrorType.EMPLOYEE_NOT_LINKED);
            }
            return clientRepository.findAllByManagerEmployeeId(userDetails.getEmployeeId()).stream()
                    .map(ClientListResponse::from)
                    .toList();
        }

        // 그 외 권한은 접근 거부
        throw new CoreException(ErrorType.ACCESS_DENIED);
    }

    @Transactional(readOnly = true)
    public ClientDetailResponse getClientDetail(Long clientId) {
        CustomUserDetails userDetails = getAuthenticatedUser();

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));

        Role role = userDetails.getRole();

        // 관리자인 경우 전체 조회 허용
        if (role == Role.ADMIN) {
            return ClientDetailResponse.from(client);
        }

        // 영업사원인 경우 본인 담당 거래처만 조회 가능
        if (role == Role.SALES_REP) {
            if (userDetails.getEmployeeId() == null) {
                throw new CoreException(ErrorType.EMPLOYEE_NOT_LINKED);
            }
            if (client.getManagerEmployee() == null
                    || !client.getManagerEmployee().getId().equals(userDetails.getEmployeeId())) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
            return ClientDetailResponse.from(client);
        }

        throw new CoreException(ErrorType.ACCESS_DENIED);
    }

    @Transactional(readOnly = true)
    public ClientProfileResponse getMyClientProfile() {
        CustomUserDetails userDetails = getAuthenticatedUser();
        requireRole(userDetails, Role.CLIENT);

        if (userDetails.getClientId() == null) {
            throw new CoreException(ErrorType.CLIENT_NOT_FOUND);
        }

        Client client = clientRepository.findById(userDetails.getClientId())
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));

        return ClientProfileResponse.from(client);
    }

    @Transactional(readOnly = true)
    public AssignedEmployeeResponse getAssignedEmployee(Long clientId) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        Role role = userDetails.getRole();

        // 권한 체크: ADMIN은 통과, CLIENT는 본인의 clientId만 가능
        if (role == Role.CLIENT) {
            if (userDetails.getClientId() == null || !userDetails.getClientId().equals(clientId)) {
                throw new CoreException(ErrorType.ACCESS_DENIED);
            }
        } else if (role != Role.ADMIN) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));

        Employee manager = client.getManagerEmployee();
        return AssignedEmployeeResponse.from(manager);
    }

    @Transactional(readOnly = true)
    public List<UnregisteredEmployeeResponse> getUnregisteredEmployees() {
        CustomUserDetails userDetails = getAuthenticatedUser();
        requireRole(userDetails, Role.ADMIN);

        return employeeRepository.findAllUnregistered().stream()
                .map(UnregisteredEmployeeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UnregisteredClientResponse> getUnregisteredClients() {
        CustomUserDetails userDetails = getAuthenticatedUser();
        requireRole(userDetails, Role.ADMIN);

        return clientRepository.findAllUnregistered().stream()
                .map(UnregisteredClientResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeSimpleResponse> getAllEmployeesSimple() {
        CustomUserDetails userDetails = getAuthenticatedUser();
        requireRole(userDetails, Role.ADMIN);

        return employeeRepository.findAllNonAdmin(Role.ADMIN).stream()
                .map(EmployeeSimpleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeManagedClientResponse> getManagedClientsByAdmin(Long employeeId) {
        CustomUserDetails userDetails = getAuthenticatedUser();
        requireRole(userDetails, Role.ADMIN);

        // 영업사원 존재 여부 확인
        if (!employeeRepository.existsById(employeeId)) {
            throw new CoreException(ErrorType.EMPLOYEE_NOT_FOUND);
        }

        return clientRepository.findAllByManagerEmployeeId(employeeId).stream()
                .map(EmployeeManagedClientResponse::from)
                .toList();
    }

    private CustomUserDetails getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        return userDetails;
    }

    private void requireRole(CustomUserDetails userDetails, Role role) {
        if (userDetails.getRole() != role) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
    }

}
