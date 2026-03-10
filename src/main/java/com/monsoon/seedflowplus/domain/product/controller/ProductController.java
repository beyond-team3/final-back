package com.monsoon.seedflowplus.domain.product.controller;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.product.dto.request.ProductRequest;
import com.monsoon.seedflowplus.domain.product.dto.request.ProductSearchCondition;
import com.monsoon.seedflowplus.domain.product.dto.response.CategoryResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductResponse;
import com.monsoon.seedflowplus.domain.product.dto.request.SaveCompareHistoryRequest;
import com.monsoon.seedflowplus.domain.product.dto.response.CompareHistoryResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.SimilarProductResponse;
import com.monsoon.seedflowplus.domain.product.service.ProductBookmarkService;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.service.ProductReadService;
import com.monsoon.seedflowplus.domain.product.service.ProductSimilarityService;
import com.monsoon.seedflowplus.domain.product.service.ProductWriteService;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductWriteService productWriteService;
    private final ProductReadService productReadService;
    private final ProductBookmarkService productBookmarkService;
    private final UserRepository userRepository;
    private final ProductSimilarityService productSimilarityService;

    // 상품 등록
    // 🌟 핵심: 파일 업로드를 위해 consumes 속성을 MULTIPART_FORM_DATA_VALUE로 지정합니다.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @io.swagger.v3.oas.annotations.Operation(summary = "상품 등록", description = "상품 정보와 이미지를 함께 등록하는 API 입니다.")
    public ResponseEntity<Long> createProduct(

            // 🌟 1. JSON 데이터: @RequestBody 대신 @RequestPart를 사용합니다.
            @Valid @RequestPart(value = "request") ProductRequest request,

            // 🌟 2. 이미지 파일: MultipartFile 객체로 받습니다. (이미지가 필수가 아니라면 required = false)
            @RequestPart(value = "productImage", required = false) MultipartFile productImage

    ) {
        // 서비스로 JSON 데이터와 사진 파일을 같이 넘겨줍니다!
        Long productId = productWriteService.createProduct(request, productImage);

        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }

    // 상품 수정
    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @io.swagger.v3.oas.annotations.Operation(summary = "상품 수정", description = "상품 수정페이지 입니다.")
    public ResponseEntity<Void> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestPart(value = "request") ProductRequest request,
            @RequestPart(value = "productImage", required = false) MultipartFile productImage,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserIdFromUserDetails(userDetails);
        productWriteService.updateProduct(productId, request, productImage, userId);
        return ResponseEntity.noContent().build();
    }

    // 상품 삭제
    @DeleteMapping("/{productId}")
    @io.swagger.v3.oas.annotations.Operation(summary = "상품 삭제", description = "상품 삭제 버튼 입니다.")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productWriteService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    // 상품 전체 목록 조회 (추후 성능 비교를 위해 임시로 전체 조회 사용)
    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "상품 전체목록 조회", description = "상품 리스트를 반환합니다.")
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @io.swagger.v3.oas.annotations.Parameter(description = "10~500 숫자") @RequestParam(defaultValue = "10") int limit,
            @org.springdoc.core.annotations.ParameterObject @ModelAttribute ProductSearchCondition condition,
            @AuthenticationPrincipal UserDetails userDetails) {
        Role role = extractRoleFromUserDetails(userDetails);

        List<ProductResponse> responses = productReadService.getAllProducts(role, condition);
        return ResponseEntity.ok(responses);
    }

    // 상품 비교하기 페이지
    @GetMapping("/compare")
    @io.swagger.v3.oas.annotations.Operation(summary = "상품 비교", description = "비교함 페이지를 반환합니다.")
    public ResponseEntity<List<ProductResponse>> getCompareProducts(
            @RequestParam List<Long> productIds,
            @AuthenticationPrincipal UserDetails userDetails) {
        Role role = extractRoleFromUserDetails(userDetails);

        List<ProductResponse> responses = productReadService.getCompareProducts(productIds, role);
        return ResponseEntity.ok(responses);
    }

    // 상품 비교 내역 저장
    @PostMapping("/compare")
    @io.swagger.v3.oas.annotations.Operation(summary = "상품 비교 저장", description = "상품 비교 내역 저장합니다.")
    public ResponseEntity<Long> saveCompareHistory(
            @Valid @RequestBody SaveCompareHistoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserIdFromUserDetails(userDetails);

        List<Long> productIds = request.getProductIds();
        String title = request.getTitle();

        Long compareId = productWriteService.saveCompareHistory(userId, productIds, title);
        return ResponseEntity.ok(compareId);
    }

    // 상품 비교 내역 히스토리 목록 조회
    @GetMapping("/compare/history")
    @io.swagger.v3.oas.annotations.Operation(summary = "상품 비교 내역", description = "상품 비교 내역 조회합니다.")
    public ResponseEntity<List<CompareHistoryResponse>> getCompareHistories(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserIdFromUserDetails(userDetails);
        Role role = extractRoleFromUserDetails(userDetails);

        List<CompareHistoryResponse> responses = productReadService.getCompareHistories(userId, role);
        return ResponseEntity.ok(responses);
    }

    // 비교 내역 삭제
    @DeleteMapping("/compare/{compareId}")
    @io.swagger.v3.oas.annotations.Operation(summary = "상품 비교 삭제", description = "상품 비교 내역 삭제합니다.")
    public ResponseEntity<Void> deleteCompareHistory(
            @PathVariable Long compareId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserIdFromUserDetails(userDetails);

        productWriteService.deleteCompareHistory(userId, compareId);
        return ResponseEntity.noContent().build();
    }

    // 상품 상세 조회
    @GetMapping("/{productId}")
    @io.swagger.v3.oas.annotations.Operation(summary = "상품 상세 조회", description = "상품 상세 페이지를 반환합니다.")
    public ResponseEntity<ProductResponse> getProductDetail(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Role role = extractRoleFromUserDetails(userDetails);

        ProductResponse response = productReadService.getProductDetail(productId, role);
        return ResponseEntity.ok(response);
    }

    // 유사도 분석
    @GetMapping("/{productId}/similar")
    @io.swagger.v3.oas.annotations.Operation(summary = "유사 상품 추천", description = "태그·재배적기·카테고리 기준 유사도를 계산하여 상위 N개 상품을 반환합니다.")
    public ResponseEntity<SimilarProductResponse> getSimilarProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int threshold,
            @RequestParam(required = false) List<String> criteria) {

        SimilarProductResponse response = productSimilarityService
                .getSimilarProducts(productId, limit, threshold, criteria);
        return ResponseEntity.ok(response);
    }

    // 상품 즐겨찾기 토글
    @PostMapping("/{productId}/bookmark")
    @io.swagger.v3.oas.annotations.Operation(summary = "상품 즐겨찾기 버튼", description = "상품을 즐겨찾기 목록에 등록합니다.")
    public ResponseEntity<Void> toggleBookmark(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserIdFromUserDetails(userDetails);

        productBookmarkService.toggleBookmark(userId, productId);
        return ResponseEntity.ok().build();
    }

    // 상품 즐겨찾기 목록 조회
    @GetMapping("/bookmarks")
    @io.swagger.v3.oas.annotations.Operation(summary = "즐겨찾기 목록 조회", description = "즐겨찾기 목록 조회합니다.")
    public ResponseEntity<List<ProductResponse>> getBookmarkedProducts(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserIdFromUserDetails(userDetails);
        Role role = extractRoleFromUserDetails(userDetails);

        List<ProductResponse> responses = productReadService.getBookmarkedProducts(userId, role);
        return ResponseEntity.ok(responses);
    }

    // Role 추출 헬퍼 메서드
    private Role extractRoleFromUserDetails(UserDetails userDetails) {
        if (userDetails instanceof User user) {
            return user.getRole();
        }

        // 권한 문자열을 기반으로 Role Enum 검색
        String authority = userDetails.getAuthorities().iterator().next().getAuthority();
        try {
            // "ROLE_ADMIN" 형식인 경우 "ADMIN"으로 잘라내어 사용 (설정에 따라 다름)
            if (authority.startsWith("ROLE_")) {
                return Role.valueOf(authority.substring(5));
            }
            return Role.valueOf(authority);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Role.CLIENT; // 기본 권한 반환 또는 예외 처리
        }
    }

    // UserId 추출 헬퍼 메서드
    private Long extractUserIdFromUserDetails(UserDetails userDetails) {
        if (userDetails instanceof User user) {
            return user.getId();
        }

        // 로그인 ID (Username)로 DB에서 유저 조회 후 ID 반환
        return userRepository.findByLoginId(userDetails.getUsername())
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND))
                .getId();
    }

    // 상품 카테고리 목록 조회 (프론트엔드 드롭다운 등 사용)
    @GetMapping("/categories")
    @io.swagger.v3.oas.annotations.Operation(summary = "상품 카테고리 목록 조회", description = "검색 및 등록 등에서 사용할 품종(수박, 고추 등) 한글명 리스트를 반환합니다.")
    public ResponseEntity<List<CategoryResponse>> getProductCategories() {
        List<CategoryResponse> categories = java.util.Arrays.stream(ProductCategory.values())
                .map(category -> CategoryResponse.builder()
                        .code(category.name())
                        .name(category.getDescription())
                        .build())
                .toList();

        return ResponseEntity.ok(categories);
    }

}
