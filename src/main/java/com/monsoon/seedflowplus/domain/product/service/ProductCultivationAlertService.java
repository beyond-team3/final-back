package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientCrop;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.ClientCropRepository;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductCalendarRecommendationResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductHarvestImminentResponse;
import com.monsoon.seedflowplus.domain.product.entity.CultivationTime;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.repository.CultivationTimeRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductCultivationAlertService {

    private static final LocalTime DEFAULT_NOTIFICATION_TIME = LocalTime.of(9, 0);

    private final ProductRepository productRepository;
    private final CultivationTimeRepository cultivationTimeRepository;
    private final ClientRepository clientRepository;
    private final ClientCropRepository clientCropRepository;
    private final UserRepository userRepository;

    public ProductCalendarRecommendationResponse getCalendarRecommendations(Integer month) {
        int targetMonth = resolveMonth(month);
        List<Product> products = productRepository.findAll();
        Map<Long, List<CultivationTime>> cultivationTimeMap = getCultivationTimeMap(products);

        List<ProductCalendarRecommendationResponse.RecommendedProductItem> items = products.stream()
                .map(product -> buildRecommendationItem(product, cultivationTimeMap.get(product.getId()), targetMonth))
                .flatMap(Optional::stream)
                .sorted(Comparator
                        .comparing(
                                ProductCalendarRecommendationResponse.RecommendedProductItem::getPlantingStart,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(
                                ProductCalendarRecommendationResponse.RecommendedProductItem::getProductCategoryLabel,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(
                                ProductCalendarRecommendationResponse.RecommendedProductItem::getProductName,
                                Comparator.nullsLast(String::compareTo)))
                .toList();

        return ProductCalendarRecommendationResponse.builder()
                .month(targetMonth)
                .items(items)
                .build();
    }

    public ProductHarvestImminentResponse getHarvestImminent(Integer month, Long employeeId) {
        int targetMonth = resolveMonth(month);
        int nextMonth = nextMonth(targetMonth);

        List<Client> managedClients = clientRepository.findAllByManagerEmployeeId(employeeId);
        if (managedClients.isEmpty()) {
            return ProductHarvestImminentResponse.builder()
                    .month(targetMonth)
                    .nextMonth(nextMonth)
                    .clients(List.of())
                    .build();
        }

        List<Product> products = productRepository.findAll();
        Map<Long, List<CultivationTime>> cultivationTimeMap = getCultivationTimeMap(products);
        Map<Long, List<ClientCrop>> clientCropMap = getClientCropMap(managedClients);

        List<ProductHarvestImminentResponse.ClientHarvestImminentItem> clientItems = managedClients.stream()
                .map(client -> buildHarvestClientItem(client, clientCropMap.get(client.getId()), products,
                        cultivationTimeMap, targetMonth, nextMonth))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(
                        ProductHarvestImminentResponse.ClientHarvestImminentItem::getClientName,
                        Comparator.nullsLast(String::compareTo)))
                .toList();

        return ProductHarvestImminentResponse.builder()
                .month(targetMonth)
                .nextMonth(nextMonth)
                .clients(clientItems)
                .build();
    }

    public List<CultivationNotificationCandidate> getNotificationCandidates(LocalDateTime now) {
        List<User> salesRepUsers = userRepository.findAllByRole(Role.SALES_REP).stream()
                .filter(user -> user.getEmployee() != null)
                .toList();
        if (salesRepUsers.isEmpty()) {
            return List.of();
        }

        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return List.of();
        }

        Map<Long, List<CultivationTime>> cultivationTimeMap = getCultivationTimeMap(products);
        List<CultivationNotificationCandidate> candidates = new ArrayList<>();

        for (User salesRepUser : salesRepUsers) {
            List<Client> managedClients = clientRepository.findAllByManagerEmployeeId(salesRepUser.getEmployee().getId());
            if (managedClients.isEmpty()) {
                continue;
            }

            Map<Long, List<ClientCrop>> clientCropMap = getClientCropMap(managedClients);
            candidates.addAll(buildNotificationCandidatesForSalesRep(
                    salesRepUser,
                    managedClients,
                    clientCropMap,
                    products,
                    cultivationTimeMap,
                    now));
        }

        return candidates;
    }

    private List<CultivationNotificationCandidate> buildNotificationCandidatesForSalesRep(
            User salesRepUser,
            List<Client> managedClients,
            Map<Long, List<ClientCrop>> clientCropMap,
            List<Product> products,
            Map<Long, List<CultivationTime>> cultivationTimeMap,
            LocalDateTime now
    ) {
        Map<String, CandidateAccumulator> candidateMap = new LinkedHashMap<>();

        for (Client client : managedClients) {
            for (ClientCrop clientCrop : clientCropMap.getOrDefault(client.getId(), List.of())) {
                for (Product product : products) {
                    if (!matchesClientCrop(clientCrop.getCropName(), product)) {
                        continue;
                    }

                    chooseSowingCandidate(product, cultivationTimeMap.get(product.getId()), now)
                            .ifPresent(selection -> accumulateCandidate(
                                    candidateMap,
                                    salesRepUser.getId(),
                                    client.getId(),
                                    product,
                                    selection));

                    chooseHarvestCandidate(product, cultivationTimeMap.get(product.getId()), now)
                            .ifPresent(selection -> accumulateCandidate(
                                    candidateMap,
                                    salesRepUser.getId(),
                                    client.getId(),
                                    product,
                                    selection));
                }
            }
        }

        return candidateMap.values().stream()
                .map(CandidateAccumulator::toCandidate)
                .sorted(Comparator.comparing(CultivationNotificationCandidate::getScheduledAt)
                        .thenComparing(CultivationNotificationCandidate::getUserId)
                        .thenComparing(CultivationNotificationCandidate::getProductId))
                .toList();
    }

    private void accumulateCandidate(
            Map<String, CandidateAccumulator> candidateMap,
            Long userId,
            Long clientId,
            Product product,
            CandidateSelection selection
    ) {
        String key = selection.type() + ":" + userId + ":" + product.getId();
        candidateMap.compute(key, (ignored, current) -> {
            if (current == null) {
                return CandidateAccumulator.create(userId, clientId, product, selection);
            }
            current.merge(clientId, selection);
            return current;
        });
    }

    private Optional<CandidateSelection> chooseSowingCandidate(
            Product product,
            List<CultivationTime> cultivationTimes,
            LocalDateTime now
    ) {
        if (cultivationTimes == null || cultivationTimes.isEmpty()) {
            return Optional.empty();
        }

        return cultivationTimes.stream()
                .filter(ct -> ct.getSowingStart() != null)
                .map(ct -> {
                    LocalDateTime scheduledAt = toSowingPromotionSchedule(ct.getSowingStart(), now);
                    return new CandidateSelection(
                            NotificationType.CULTIVATION_SOWING_PROMOTION,
                            ct.getSowingStart(),
                            scheduledAt,
                            ct);
                })
                .filter(selection -> shouldCreateAtRun(selection.scheduledAt(), now))
                .min(Comparator.comparing(CandidateSelection::scheduledAt)
                        .thenComparing(selection -> selection.cultivationTime().getSowingStart(),
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ignored -> product.getProductName(), Comparator.nullsLast(String::compareTo)));
    }

    private Optional<CandidateSelection> chooseHarvestCandidate(
            Product product,
            List<CultivationTime> cultivationTimes,
            LocalDateTime now
    ) {
        if (cultivationTimes == null || cultivationTimes.isEmpty()) {
            return Optional.empty();
        }

        return cultivationTimes.stream()
                .filter(ct -> ct.getHarvestingStart() != null)
                .map(ct -> {
                    LocalDateTime scheduledAt = toHarvestFeedbackSchedule(ct.getHarvestingStart(), now);
                    return new CandidateSelection(
                            NotificationType.CULTIVATION_HARVEST_FEEDBACK,
                            ct.getHarvestingStart(),
                            scheduledAt,
                            ct);
                })
                .filter(selection -> shouldCreateAtRun(selection.scheduledAt(), now))
                .min(Comparator.comparing(CandidateSelection::scheduledAt)
                        .thenComparing(selection -> selection.cultivationTime().getHarvestingStart(),
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ignored -> product.getProductName(), Comparator.nullsLast(String::compareTo)));
    }

    private boolean shouldCreateAtRun(LocalDateTime scheduledAt, LocalDateTime now) {
        return !scheduledAt.toLocalDate().isAfter(now.toLocalDate());
    }

    private LocalDateTime toSowingPromotionSchedule(Integer sowingStartMonth, LocalDateTime now) {
        LocalDate seasonStart = resolveNextOccurrence(now.toLocalDate(), sowingStartMonth);
        return LocalDateTime.of(seasonStart.minusMonths(1).withDayOfMonth(1), DEFAULT_NOTIFICATION_TIME);
    }

    private LocalDateTime toHarvestFeedbackSchedule(Integer harvestingStartMonth, LocalDateTime now) {
        LocalDate harvestStart = resolveNextOccurrence(now.toLocalDate(), harvestingStartMonth);
        return LocalDateTime.of(harvestStart.withDayOfMonth(1), DEFAULT_NOTIFICATION_TIME);
    }

    private LocalDate resolveNextOccurrence(LocalDate today, Integer month) {
        int year = month < today.getMonthValue() ? today.getYear() + 1 : today.getYear();
        return LocalDate.of(year, month, 1);
    }

    private Map<Long, List<CultivationTime>> getCultivationTimeMap(Collection<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();
        return cultivationTimeRepository.findAllByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(ct -> ct.getProduct().getId()));
    }

    private Map<Long, List<ClientCrop>> getClientCropMap(List<Client> managedClients) {
        if (managedClients == null || managedClients.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> clientIds = managedClients.stream().map(Client::getId).toList();
        return clientCropRepository.findAllByClientIdIn(clientIds).stream()
                .collect(Collectors.groupingBy(crop -> crop.getClient().getId()));
    }

    private Optional<ProductCalendarRecommendationResponse.RecommendedProductItem> buildRecommendationItem(
            Product product,
            List<CultivationTime> cultivationTimes,
            int targetMonth
    ) {
        if (cultivationTimes == null || cultivationTimes.isEmpty()) {
            return Optional.empty();
        }

        return cultivationTimes.stream()
                .filter(ct -> isBetweenInclusive(targetMonth, ct.getSowingStart(), ct.getPlantingStart()))
                .min(Comparator.comparing(CultivationTime::getPlantingStart, Comparator.nullsLast(Integer::compareTo)))
                .map(ct -> ProductCalendarRecommendationResponse.RecommendedProductItem.builder()
                        .productId(product.getId())
                        .productName(product.getProductName())
                        .productCategory(product.getProductCategory().name())
                        .productCategoryLabel(product.getProductCategory().getDescription())
                        .description(product.getProductDescription())
                        .imageUrl(product.getProductImageUrl())
                        .sowingStart(ct.getSowingStart())
                        .plantingStart(ct.getPlantingStart())
                        .croppingSystem(ct.getCroppingSystem())
                        .region(ct.getRegion())
                        .build());
    }

    private Optional<ProductHarvestImminentResponse.ClientHarvestImminentItem> buildHarvestClientItem(
            Client client,
            List<ClientCrop> clientCrops,
            List<Product> products,
            Map<Long, List<CultivationTime>> cultivationTimeMap,
            int targetMonth,
            int nextMonth
    ) {
        if (clientCrops == null || clientCrops.isEmpty()) {
            return Optional.empty();
        }

        List<ProductHarvestImminentResponse.CropHarvestImminentItem> cropItems = clientCrops.stream()
                .map(clientCrop -> buildHarvestCropItem(clientCrop, products, cultivationTimeMap, targetMonth, nextMonth))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(
                        ProductHarvestImminentResponse.CropHarvestImminentItem::getCropName,
                        Comparator.nullsLast(String::compareTo)))
                .toList();

        if (cropItems.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(ProductHarvestImminentResponse.ClientHarvestImminentItem.builder()
                .clientId(client.getId())
                .clientName(client.getClientName())
                .crops(cropItems)
                .build());
    }

    private Optional<ProductHarvestImminentResponse.CropHarvestImminentItem> buildHarvestCropItem(
            ClientCrop clientCrop,
            List<Product> products,
            Map<Long, List<CultivationTime>> cultivationTimeMap,
            int targetMonth,
            int nextMonth
    ) {
        LinkedHashMap<Long, ProductHarvestImminentResponse.HarvestProductItem> matchedProducts = new LinkedHashMap<>();

        for (Product product : products) {
            if (!matchesClientCrop(clientCrop.getCropName(), product)) {
                continue;
            }

            pickHarvestWindow(product, cultivationTimeMap.get(product.getId()), targetMonth, nextMonth)
                    .ifPresent(item -> matchedProducts.put(product.getId(), item));
        }

        if (matchedProducts.isEmpty()) {
            return Optional.empty();
        }

        List<ProductHarvestImminentResponse.HarvestProductItem> items = matchedProducts.values().stream()
                .sorted(Comparator
                        .comparing(
                                ProductHarvestImminentResponse.HarvestProductItem::getHarvestingStart,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(
                                ProductHarvestImminentResponse.HarvestProductItem::getProductName,
                                Comparator.nullsLast(String::compareTo)))
                .toList();

        return Optional.of(ProductHarvestImminentResponse.CropHarvestImminentItem.builder()
                .cropName(clientCrop.getCropName())
                .matchedProducts(items)
                .build());
    }

    private Optional<ProductHarvestImminentResponse.HarvestProductItem> pickHarvestWindow(
            Product product,
            List<CultivationTime> cultivationTimes,
            int targetMonth,
            int nextMonth
    ) {
        if (cultivationTimes == null || cultivationTimes.isEmpty()) {
            return Optional.empty();
        }

        return cultivationTimes.stream()
                .filter(ct -> isHarvestImminent(ct, targetMonth, nextMonth))
                .sorted(Comparator
                        .comparing((CultivationTime ct) -> harvestPriority(ct, targetMonth))
                        .thenComparing(CultivationTime::getHarvestingStart,
                                Comparator.nullsLast(Integer::compareTo)))
                .findFirst()
                .map(ct -> ProductHarvestImminentResponse.HarvestProductItem.builder()
                        .productId(product.getId())
                        .productName(product.getProductName())
                        .productCategory(product.getProductCategory().name())
                        .productCategoryLabel(product.getProductCategory().getDescription())
                        .imageUrl(product.getProductImageUrl())
                        .harvestingStart(ct.getHarvestingStart())
                        .harvestingEnd(ct.getHarvestingEnd())
                        .croppingSystem(ct.getCroppingSystem())
                        .region(ct.getRegion())
                        .build());
    }

    private boolean matchesClientCrop(String cropName, Product product) {
        String normalizedCropName = normalize(cropName);
        if (normalizedCropName.isEmpty()) {
            return false;
        }

        String categoryLabel = normalize(product.getProductCategory().getDescription());
        String productName = normalize(product.getProductName());
        String categoryName = normalize(product.getProductCategory().name());

        return normalizedCropName.equals(categoryLabel)
                || normalizedCropName.equals(categoryName)
                || productName.contains(normalizedCropName)
                || normalizedCropName.contains(productName);
    }

    private boolean isHarvestImminent(CultivationTime cultivationTime, int targetMonth, int nextMonth) {
        Integer harvestingStart = cultivationTime.getHarvestingStart();
        Integer harvestingEnd = cultivationTime.getHarvestingEnd();
        if (harvestingStart == null || harvestingEnd == null) {
            return false;
        }
        return isBetweenInclusive(targetMonth, harvestingStart, harvestingEnd)
                || isBetweenInclusive(nextMonth, harvestingStart, harvestingEnd);
    }

    private int harvestPriority(CultivationTime cultivationTime, int targetMonth) {
        return isBetweenInclusive(targetMonth, cultivationTime.getHarvestingStart(),
                cultivationTime.getHarvestingEnd()) ? 0 : 1;
    }

    private boolean isBetweenInclusive(int targetMonth, Integer start, Integer end) {
        if (start == null || end == null) {
            return false;
        }
        return start <= targetMonth && targetMonth <= end;
    }

    private int resolveMonth(Integer month) {
        return month != null ? month : LocalDate.now().getMonthValue();
    }

    private int nextMonth(int targetMonth) {
        return targetMonth == 12 ? 1 : targetMonth + 1;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private record CandidateSelection(
            NotificationType type,
            Integer referenceMonth,
            LocalDateTime scheduledAt,
            CultivationTime cultivationTime
    ) {
    }

    private static final class CandidateAccumulator {
        private final Long userId;
        private final Long productId;
        private final String productName;
        private NotificationType type;
        private Integer referenceMonth;
        private LocalDateTime scheduledAt;
        private final Set<Long> clientIds;

        private CandidateAccumulator(
                Long userId,
                Long productId,
                String productName,
                NotificationType type,
                Integer referenceMonth,
                LocalDateTime scheduledAt,
                Set<Long> clientIds
        ) {
            this.userId = userId;
            this.productId = productId;
            this.productName = productName;
            this.type = type;
            this.referenceMonth = referenceMonth;
            this.scheduledAt = scheduledAt;
            this.clientIds = clientIds;
        }

        private static CandidateAccumulator create(
                Long userId,
                Long clientId,
                Product product,
                CandidateSelection selection
        ) {
            Set<Long> clientIds = new HashSet<>();
            clientIds.add(clientId);
            return new CandidateAccumulator(
                    userId,
                    product.getId(),
                    product.getProductName(),
                    selection.type(),
                    selection.referenceMonth(),
                    selection.scheduledAt(),
                    clientIds);
        }

        private void merge(Long clientId, CandidateSelection selection) {
            clientIds.add(clientId);
            if (selection.scheduledAt().isBefore(scheduledAt)) {
                scheduledAt = selection.scheduledAt();
                referenceMonth = selection.referenceMonth();
                type = selection.type();
            }
        }

        private CultivationNotificationCandidate toCandidate() {
            return CultivationNotificationCandidate.builder()
                    .type(type)
                    .userId(userId)
                    .productId(productId)
                    .productName(productName)
                    .referenceMonth(referenceMonth)
                    .clientCount(clientIds.size())
                    .scheduledAt(scheduledAt)
                    .build();
        }
    }
}
