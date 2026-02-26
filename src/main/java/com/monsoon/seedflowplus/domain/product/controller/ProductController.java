package com.monsoon.seedflowplus.domain.product.controller;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.product.dto.request.ProductRequest;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductResponse;
import com.monsoon.seedflowplus.domain.product.service.ProductBookmarkService;
import com.monsoon.seedflowplus.domain.product.service.ProductReadService;
import com.monsoon.seedflowplus.domain.product.service.ProductWriteService;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductWriteService productWriteService;
    private final ProductReadService productReadService;
    private final ProductBookmarkService productBookmarkService;
    private final UserRepository userRepository;

    // 상품 등록
    @PostMapping
    public ResponseEntity<Long> createProduct(@Valid @RequestBody ProductRequest request) {
        Long productId = productWriteService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }

    // 상품 수정
    @PutMapping("/{productId}")
    public ResponseEntity<Void> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequest request) {
        productWriteService.updateProduct(productId, request);
        return ResponseEntity.noContent().build();
    }

    // 상품 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productWriteService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    // 상품 전체 목록 조회
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @AuthenticationPrincipal UserDetails userDetails) {
        // UserDetails를 User 엔티티 또는 인증된 사용자 객체로 캐스팅하여 Role 획득 필요
        // 현재는 UserDetails의 권한 정보를 바탕으로 Role 매핑 (예시 구현)
        Role role = extractRoleFromUserDetails(userDetails);

        List<ProductResponse> responses = productReadService.getAllProducts(role);
        return ResponseEntity.ok(responses);
    }

    // 상품 비교하기 페이지
    @GetMapping("/compare")
    public ResponseEntity<List<ProductResponse>> getCompareProducts(
            @RequestParam List<Long> productIds,
            @AuthenticationPrincipal UserDetails userDetails) {
        Role role = extractRoleFromUserDetails(userDetails);

        List<ProductResponse> responses = productReadService.getCompareProducts(productIds, role);
        return ResponseEntity.ok(responses);
    }

    // 상품 상세 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProductDetail(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Role role = extractRoleFromUserDetails(userDetails);

        ProductResponse response = productReadService.getProductDetail(productId, role);
        return ResponseEntity.ok(response);
    }

    // 상품 즐겨찾기 토글
    @PostMapping("/{productId}/bookmark")
    public ResponseEntity<Void> toggleBookmark(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserIdFromUserDetails(userDetails);

        productBookmarkService.toggleBookmark(userId, productId);
        return ResponseEntity.ok().build();
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
}
