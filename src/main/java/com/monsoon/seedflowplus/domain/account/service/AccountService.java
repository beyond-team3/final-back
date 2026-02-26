package com.monsoon.seedflowplus.domain.account.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.dto.request.ClientRegisterRequest;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private  final ClientRepository clientRepository;

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

}
