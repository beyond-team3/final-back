package com.monsoon.seedflowplus.domain.map.service;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.map.dto.request.PestMapSearchRequest;
import com.monsoon.seedflowplus.domain.map.dto.response.PestMapSearchResponse;
import com.monsoon.seedflowplus.domain.map.dto.response.SalesOfficeResponse;
import com.monsoon.seedflowplus.domain.map.repository.PestForecastRepository;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PestMapService {

    private final PestForecastRepository forecastRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;

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

        // 카테고리가 정상적으로 매핑되었을 때만 DB 조회 실행
        if (category != null) {
            products = productRepository.findByProductCategory(category)
                    .stream()
                    .filter(p -> isProductResistantToPest(p, request.getPestCode()))
                    .map(p -> PestMapSearchResponse.ProductDto.builder()
                            .name(p.getProductName())
                            .description(p.getProductDescription())
                            .resistance(extractResistanceTag(p))
                            .isFavorite(false)
                            .build())
                    .collect(Collectors.toList());
        }

        return PestMapSearchResponse.builder()
                .forecasts(forecasts)
                .recommendedProducts(products)
                .build();
    }

    public List<SalesOfficeResponse> getAllSalesOffices() {
        return clientRepository.findAll().stream()
                .filter(client -> client.getLatitude() != null && client.getLongitude() != null)
                .map(client -> SalesOfficeResponse.builder()
                        .id(client.getId().toString())
                        .name(client.getClientName())
                        .lat(client.getLatitude())
                        .lng(client.getLongitude())
                        .score(calculateVisitScore(client))
                        .handledCrops(
                                client.getCrops().stream()
                                        .map(crop -> crop.getCropName())
                                        .toArray(String[]::new)
                        )
                        .build())
                .collect(Collectors.toList());
    }

    private boolean isProductResistantToPest(Product product, String pestCode) {
        if (product.getTags() == null || !product.getTags().containsKey("내병성")) return false;
        String pestName = mapPestCodeToName(pestCode);
        return product.getTags().get("내병성").stream()
                .anyMatch(tag -> tag.contains(pestName) || tag.contains(pestCode));
    }

    private String extractResistanceTag(Product product) {
        if (product.getTags() == null || !product.getTags().containsKey("내병성")) return "";
        return String.join(", ", product.getTags().get("내병성"));
    }

    private Integer calculateVisitScore(Client client) {
        if (client.getTotalCredit() == null || client.getTotalCredit().doubleValue() == 0) return 50;
        double usageRatio = client.getUsedCredit().doubleValue() / client.getTotalCredit().doubleValue();
        return (int) (usageRatio * 100);
    }

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
        return switch (pestCode) {
            case "PP01" -> "탄저병";
            case "PP02", "TM01" -> "역병";
            case "CB01", "RD01" -> "무름병";
            case "CB03", "GR01" -> "노균병";
            default -> pestCode;
        };
    }
}