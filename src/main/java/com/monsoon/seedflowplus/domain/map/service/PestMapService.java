package com.monsoon.seedflowplus.domain.map.service;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.map.dto.request.PestMapSearchRequest;
import com.monsoon.seedflowplus.domain.map.dto.response.PestMapSearchResponse;
import com.monsoon.seedflowplus.domain.map.dto.response.SalesOfficeResponse;
import com.monsoon.seedflowplus.domain.map.repository.PestForecastRepository;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductBookmark;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.repository.ProductBookmarkRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PestMapService {

    private final PestForecastRepository forecastRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final ProductBookmarkRepository bookmarkRepository;

    public PestMapSearchResponse getPestMapData(PestMapSearchRequest request) {

        // 1. 예찰 데이터 조회
        var forecasts = forecastRepository.findAllByPestCode(request.getPestCode())
                .stream()
                .map(f -> PestMapSearchResponse.ForecastDto.builder()
                        .areaName(f.getAreaName())
                        .severity(f.getSeverity())
                        .build())
                .collect(Collectors.toList());

        // 2. 추천 품종 데이터 조회
        ProductCategory category = mapCropCodeToCategory(request.getCropCode());
        List<PestMapSearchResponse.ProductDto> products = java.util.Collections.emptyList();

        // 3. 현재 유저의 북마크 목록 조회 (성능을 위해 미리 조회)
        Set<Long> bookmarkedProductIds = getCurrentUserBookmarkedProductIds();

        // 카테고리가 정상적으로 매핑되었을 때만 DB 조회 실행
        if (category != null) {
            String targetPestName = mapPestCodeToName(request.getPestCode());
            products = productRepository.findByProductCategory(category)
                    .stream()
                    .filter(p -> isProductResistantToPest(p, request.getPestCode(), targetPestName))
                    .map(p -> PestMapSearchResponse.ProductDto.builder()
                            .id(p.getId())
                            .name(p.getProductName())
                            .description(p.getProductDescription())
                            .resistance(extractResistanceTag(p))
                            .isFavorite(bookmarkedProductIds.contains(p.getId()))
                            .build())
                    .collect(Collectors.toList());
        }

        return PestMapSearchResponse.builder()
                .forecasts(forecasts)
                .recommendedProducts(products)
                .build();
    }

    private Set<Long> getCurrentUserBookmarkedProductIds() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return Collections.emptySet();
        }

        Long accountId = userDetails.getUserId();
        return bookmarkRepository.findByAccount_Id(accountId).stream()
                .map(bookmark -> bookmark.getProduct().getId())
                .collect(Collectors.toSet());
    }

    public List<SalesOfficeResponse> getAllSalesOffices() {
        return clientRepository.findAll().stream()
                .filter(client -> client.getLatitude() != null && client.getLongitude() != null)
                .map(client -> SalesOfficeResponse.builder()
                        .id(client.getId().toString())
                        .name(client.getClientName())
                        .lat(client.getLatitude())
                        .lng(client.getLongitude())
//                        .score(calculateVisitScore(client))
                        .handledCrops(
                                Optional.ofNullable(client.getCrops())
                                        .orElse(Collections.emptyList())
                                        .stream()
                                        .map(crop -> crop.getCropName())
                                        .toArray(String[]::new)
                        )
                        .build())
                .collect(Collectors.toList());
    }

    private boolean isProductResistantToPest(Product product, String pestCode, String pestName) {
        if (pestCode == null || product.getTags() == null || !product.getTags().containsKey("내병성")) return false;

        // 매핑 성공 여부 확인 (코드와 이름이 다르면 매핑 성공으로 간주)
        boolean isMappingSuccessful = !pestCode.equals(pestName);

        return product.getTags().get("내병성").stream()
                .anyMatch(tag -> {
                    // 1. 코드는 완전 일치 (대소문자 무관)
                    if (tag.equalsIgnoreCase(pestCode)) return true;

                    // 2. 이름(한글) 매핑 성공 시에만 부분 일치 허용
                    return isMappingSuccessful && pestName != null && tag.contains(pestName);
                });
    }

    private String extractResistanceTag(Product product) {
        if (product.getTags() == null || !product.getTags().containsKey("내병성")) return "";
        return String.join(", ", product.getTags().get("내병성"));
    }

//    private Integer calculateVisitScore(Client client) {
//        if (client.getTotalCredit() == null || client.getTotalCredit().doubleValue() == 0 || client.getUsedCredit() == null) {
//            return 50;
//        }
//        double usageRatio = client.getUsedCredit().doubleValue() / client.getTotalCredit().doubleValue();
//        return (int) (usageRatio * 100);
//    }

    private ProductCategory mapCropCodeToCategory(String cropCode) {
        if (cropCode == null || cropCode.isBlank()) return null;
        try {
            return ProductCategory.valueOf(cropCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 알 수 없는 작물 코드가 들어오면 ETC 대신 null을 반환하여 예외 방지
            return null;
        }
    }

    private String mapPestCodeToName(String pestCode) {
        if (pestCode == null || pestCode.isBlank()) return "UNKNOWN";
        
        return switch (pestCode) {
            case "P01", "CB03", "GR01" -> "노균병";
            case "P02", "CB01", "RD01" -> "무름병";
            case "P03", "PP01" -> "탄저병";
            case "P04" -> "뿌리혹병";
            case "PP02", "TM01" -> "역병";
            default -> pestCode;
        };
    }
}