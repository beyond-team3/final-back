package com.monsoon.seedflowplus.domain.statistics.billing.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.request.BillingRevenueStatisticsFilter;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.CategoryBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyCategoryBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.repository.BillingRevenueStatisticsRepository;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingRevenueStatisticsQueryService {

    private static final long MAX_RANGE_MONTHS = 24L;

    private final BillingRevenueStatisticsRepository repository;

    public List<MonthlyBilledRevenueDto> getMonthlyRevenue(BillingRevenueStatisticsFilter filter) {
        validateFilter(filter);
        return repository.findMonthlyRevenue(filter);
    }

    public List<CategoryBilledRevenueDto> getCategoryRevenue(BillingRevenueStatisticsFilter filter) {
        validateFilter(filter);
        return repository.findCategoryRevenue(filter);
    }

    public List<MonthlyCategoryBilledRevenueDto> getMonthlyCategoryRevenue(BillingRevenueStatisticsFilter filter) {
        validateFilter(filter);
        return repository.findMonthlyCategoryRevenue(filter);
    }

    private void validateFilter(BillingRevenueStatisticsFilter filter) {
        if (filter == null || filter.getFromDate() == null || filter.getToDate() == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE, "fromDate/toDate는 필수입니다.");
        }

        if (filter.getFromDate().isAfter(filter.getToDate())) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE, "fromDate는 toDate보다 이후일 수 없습니다.");
        }

        YearMonth fromMonth = YearMonth.from(filter.getFromDate());
        YearMonth toMonth = YearMonth.from(filter.getToDate());
        long inclusiveMonthCount = ChronoUnit.MONTHS.between(fromMonth, toMonth) + 1;

        if (inclusiveMonthCount > MAX_RANGE_MONTHS) {
            throw new CoreException(
                    ErrorType.INVALID_INPUT_VALUE,
                    "최대 조회 기간은 24개월입니다."
            );
        }
    }
}
