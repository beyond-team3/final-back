package com.monsoon.seedflowplus.domain.deal.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealSearchCondition;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SalesDealQueryServiceTest {

    @Mock
    private SalesDealRepository salesDealRepository;

    @Test
    @DisplayName("거래처 사용자의 deal 목록 조회는 관리자 승인 이후 문서만 보이도록 조건을 강제한다")
    void getDealsForCurrentUserMarksClientVisibilityFilter() {
        SalesDealQueryService service = new SalesDealQueryService(salesDealRepository);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.CLIENT);
        when(principal.getClientId()).thenReturn(44L);

        when(salesDealRepository.searchDeals(any(SalesDealSearchCondition.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.<SalesDeal>of()));

        service.getDealsForCurrentUser(
                SalesDealSearchCondition.builder().build(),
                PageRequest.of(0, 20),
                principal
        );

        ArgumentCaptor<SalesDealSearchCondition> conditionCaptor = ArgumentCaptor.forClass(SalesDealSearchCondition.class);
        org.mockito.Mockito.verify(salesDealRepository).searchDeals(conditionCaptor.capture(), eq(PageRequest.of(0, 20)));

        SalesDealSearchCondition captured = conditionCaptor.getValue();
        assertThat(captured.getClientId()).isEqualTo(44L);
        assertThat(captured.getClientPostAdminApprovalOnly()).isTrue();
    }
}
