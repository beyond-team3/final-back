package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.product.dto.response.SimilarProductResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.SimilarProductResponse.SimilarProductItem;
import com.monsoon.seedflowplus.domain.product.entity.CultivationTime;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.repository.CultivationTimeRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSimilarityService {

        // 태그 카테고리별 가중치 - DB 저장 키(한글) 기준
        private static final Map<String, Double> TAG_CATEGORY_WEIGHTS = Map.of(
                        "재배환경", 0.30,
                        "내병성", 0.25,
                        "생육및숙기", 0.20,
                        "과실품질", 0.15,
                        "재배편의성", 0.10);

        // 프론트 체크박스 키(영문) → DB 태그 키(한글) 매핑
        private static final Map<String, String> CRITERIA_KEY_MAP = Map.of(
                        "env", "재배환경",
                        "res", "내병성",
                        "growth", "생육및숙기",
                        "quality", "과실품질",
                        "conv", "재배편의성");

        // 유사도 전체 가중치 (같은 카테고리 내에서만 비교하므로 카테고리 가중치 제거)
        private static final double WEIGHT_TAG = 0.6;
        private static final double WEIGHT_CULTIVATION = 0.4;

        private static final int DEFAULT_LIMIT = 10;
        private static final int MAX_LIMIT = 100;

        private final ProductRepository productRepository;
        private final CultivationTimeRepository cultivationTimeRepository;

        /**
         * @param productId 기준 상품 ID
         * @param limit     반환 개수 (기본 10)
         * @param threshold 유사도 최소 임계값 0~100 (기본 0 = 필터 없음)
         * @param criteria  계산에 포함할 태그 카테고리 키 목록 (null 또는 빈 리스트 = 전체 사용)
         */
        public SimilarProductResponse getSimilarProducts(Long productId, int limit,
                        int threshold, List<String> criteria) {
                // 기준 상품 조회
                Product base = productRepository.findById(productId)
                                .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

                // 비교 대상 조회 (자기 자신 제외 + 같은 카테고리만)
                List<Product> candidates = productRepository.findAllByProductCategoryAndIdNot(
                                base.getProductCategory(), productId);

                // 재배적기 일괄 조회 → Map<productId, List<CultivationTime>>
                List<Long> candidateIds = candidates.stream().map(Product::getId).toList();
                Map<Long, List<CultivationTime>> ctMap = cultivationTimeRepository.findAllByProductIdIn(candidateIds)
                                .stream()
                                .collect(Collectors.groupingBy(ct -> ct.getProduct().getId()));

                List<CultivationTime> baseCts = cultivationTimeRepository.findByProductId(productId);

                // 파라미터 정규화
                int normalizedLimit = (limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
                int normalizedThreshold = Math.max(0, Math.min(threshold, 100));

                // criteria 영문 키 → 한글 변환 후 유효성 검증, 전부 무효 시 전체 카테고리로 fallback
                Set<String> validatedCriteria = (criteria == null || criteria.isEmpty())
                                ? Set.of()
                                : criteria.stream()
                                                .filter(Objects::nonNull)
                                                .map(String::trim)
                                                .map(key -> CRITERIA_KEY_MAP.getOrDefault(key, key)) // 영문이면 한글로 변환
                                                .filter(TAG_CATEGORY_WEIGHTS::containsKey)
                                                .collect(Collectors.toSet());

                final Set<String> activeCriteria = validatedCriteria.isEmpty()
                                ? new HashSet<>(TAG_CATEGORY_WEIGHTS.keySet())
                                : validatedCriteria;

                // 유사도 계산 → threshold 필터 → 상위 limit개 정렬
                List<SimilarProductItem> similarProducts = candidates.stream()
                                .map(candidate -> {
                                        double score = calculateTotalScore(base, baseCts, candidate,
                                                        ctMap.getOrDefault(candidate.getId(), List.of()),
                                                        activeCriteria);
                                        return SimilarProductItem.builder()
                                                        .productId(candidate.getId())
                                                        .productName(candidate.getProductName())
                                                        .category(candidate.getProductCategory().name())
                                                        .tags(candidate.getTags())
                                                        .similarityScore((int) Math.round(score * 100))
                                                        .build();
                                })
                                .filter(item -> item.getSimilarityScore() >= normalizedThreshold)
                                .sorted(Comparator.comparingInt(SimilarProductItem::getSimilarityScore).reversed())
                                .limit(normalizedLimit)
                                .toList();

                return SimilarProductResponse.builder()
                                .productId(productId)
                                .similarProducts(similarProducts)
                                .build();
        }

        // ── 최종 점수 계산 ──────────────────────────────────────────────

        private double calculateTotalScore(Product base, List<CultivationTime> baseCts,
                        Product candidate, List<CultivationTime> candidateCts,
                        Set<String> activeCriteria) {
                double tagScore = calculateTagSimilarity(base.getTags(), candidate.getTags(), activeCriteria);
                double cultivationScore = calculateCultivationSimilarity(baseCts, candidateCts);

                return (tagScore * WEIGHT_TAG)
                                + (cultivationScore * WEIGHT_CULTIVATION);
        }

        // ── 태그 유사도: 카테고리별 Jaccard 가중 합산 ───────────────────

        private double calculateTagSimilarity(Map<String, List<String>> baseTags,
                        Map<String, List<String>> candidateTags,
                        Set<String> activeCriteria) {
                if (baseTags == null || baseTags.isEmpty()
                                || candidateTags == null || candidateTags.isEmpty()) {
                        return 0.0;
                }

                double totalWeight = 0.0;
                double weightedSum = 0.0;

                for (Map.Entry<String, Double> entry : TAG_CATEGORY_WEIGHTS.entrySet()) {
                        String key = entry.getKey();
                        double weight = entry.getValue();

                        // 체크박스에서 선택되지 않은 카테고리 제외
                        if (!activeCriteria.contains(key))
                                continue;

                        Set<String> baseSet = new HashSet<>(baseTags.getOrDefault(key, List.of()));
                        Set<String> candidateSet = new HashSet<>(candidateTags.getOrDefault(key, List.of()));

                        // 둘 다 비어있으면 해당 카테고리 제외 (가중치 재분배)
                        if (baseSet.isEmpty() && candidateSet.isEmpty())
                                continue;

                        weightedSum += weight * jaccardSimilarity(baseSet, candidateSet);
                        totalWeight += weight;
                }

                // 유효한 카테고리만으로 재정규화
                return totalWeight == 0.0 ? 0.0 : weightedSum / totalWeight;
        }

        // Jaccard: 교집합 / 합집합
        private double jaccardSimilarity(Set<String> a, Set<String> b) {
                if (a.isEmpty() && b.isEmpty())
                        return 0.0;

                Set<String> intersection = new HashSet<>(a);
                intersection.retainAll(b);

                Set<String> union = new HashSet<>(a);
                union.addAll(b);

                return (double) intersection.size() / union.size();
        }

        // ── 재배적기 겹침: 파종/정식/수확 각각 겹침 계산 후 최대값 ────────
        private double calculateCultivationSimilarity(List<CultivationTime> baseCts,
                        List<CultivationTime> candidateCts) {
                if (baseCts == null || baseCts.isEmpty() || candidateCts == null || candidateCts.isEmpty())
                        return 0.0;

                double maxScore = 0.0;
                for (CultivationTime base : baseCts) {
                        for (CultivationTime candidate : candidateCts) {
                                double score = calculateSingleCultivationSimilarity(base, candidate);
                                maxScore = Math.max(maxScore, score);
                        }
                }
                return maxScore;
        }

        private double calculateSingleCultivationSimilarity(CultivationTime base, CultivationTime candidate) {
                if (base == null || candidate == null)
                        return 0.0;

                List<Double> overlaps = new ArrayList<>();

                // 파종
                Double sowing = rangeOverlapRatio(
                                base.getSowingStart(), base.getSowingEnd(),
                                candidate.getSowingStart(), candidate.getSowingEnd());
                if (sowing != null)
                        overlaps.add(sowing);

                // 정식
                Double planting = rangeOverlapRatio(
                                base.getPlantingStart(), base.getPlantingEnd(),
                                candidate.getPlantingStart(), candidate.getPlantingEnd());
                if (planting != null)
                        overlaps.add(planting);

                // 수확
                Double harvesting = rangeOverlapRatio(
                                base.getHarvestingStart(), base.getHarvestingEnd(),
                                candidate.getHarvestingStart(), candidate.getHarvestingEnd());
                if (harvesting != null)
                        overlaps.add(harvesting);

                if (overlaps.isEmpty())
                        return 0.0;

                return overlaps.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        /**
         * 두 월 범위의 겹침 비율 계산 (연도 경계 순환 구간 처리)
         * ex) 11~2월(겨울 파종) 같은 연말-연초 구간도 정확하게 계산
         */
        private Double rangeOverlapRatio(Integer startA, Integer endA,
                        Integer startB, Integer endB) {
                if (startA == null || endA == null || startB == null || endB == null) {
                        return null;
                }
                if (!isValidMonth(startA) || !isValidMonth(endA)
                                || !isValidMonth(startB) || !isValidMonth(endB)) {
                        return null;
                }

                Set<Integer> aMonths = expandMonths(startA, endA);
                Set<Integer> bMonths = expandMonths(startB, endB);

                Set<Integer> intersection = new HashSet<>(aMonths);
                intersection.retainAll(bMonths);

                Set<Integer> union = new HashSet<>(aMonths);
                union.addAll(bMonths);

                return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
        }

        private boolean isValidMonth(int month) {
                return month >= 1 && month <= 12;
        }

        // 월 범위를 Set으로 확장 (순환 처리: 11 → 12 → 1 → 2), 최대 12회 반복으로 무한루프 방지
        private Set<Integer> expandMonths(int start, int end) {
                Set<Integer> months = new HashSet<>();
                int cur = start;
                for (int i = 0; i < 12; i++) {
                        months.add(cur);
                        if (cur == end)
                                break;
                        cur = (cur % 12) + 1;
                }
                return months;
        }
}