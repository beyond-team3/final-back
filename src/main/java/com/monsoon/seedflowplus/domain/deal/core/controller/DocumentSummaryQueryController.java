package com.monsoon.seedflowplus.domain.deal.core.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.deal.common.DealPaginationConstants;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.common.PaginationUtils;
import com.monsoon.seedflowplus.domain.deal.core.dto.response.DocumentSummaryResponse;
import com.monsoon.seedflowplus.domain.deal.core.repository.DocumentSummarySearchCondition;
import com.monsoon.seedflowplus.domain.deal.core.service.DocumentSummaryQueryService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Documents")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/documents")
public class DocumentSummaryQueryController {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("createdAt");

    private final DocumentSummaryQueryService documentSummaryQueryService;

    @Operation(summary = "모든 문서 목록 조회")
    @GetMapping
    public ApiResult<Page<DocumentSummaryResponse>> getDocuments(
            @Parameter(description = "문서 타입")
            @RequestParam(required = false) DealType docType,
            @Parameter(description = "문서 상태")
            @RequestParam(required = false) String status,
            @Parameter(description = "문서 코드 검색어")
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        DocumentSummarySearchCondition condition = DocumentSummarySearchCondition.builder()
                .docType(docType)
                .status(status)
                .keyword(keyword)
                .build();

        Pageable pageable = PaginationUtils.parsePageRequest(
                page,
                size,
                sort,
                Sort.by(Sort.Order.desc("createdAt")),
                ALLOWED_SORT_PROPERTIES,
                DealPaginationConstants.MAX_PAGE_SIZE
        );

        return ApiResult.success(documentSummaryQueryService.getDocuments(condition, pageable, userDetails));
    }
}
