package com.monsoon.seedflowplus.domain.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsoon.seedflowplus.config.TestSecurityConfig;
import com.monsoon.seedflowplus.core.common.support.error.GlobalExceptionHandler;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import org.springframework.test.util.ReflectionTestUtils;
import com.monsoon.seedflowplus.domain.product.dto.response.CompareHistoryResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.SimilarProductResponse;
import com.monsoon.seedflowplus.domain.product.service.ProductBookmarkService;
import com.monsoon.seedflowplus.domain.product.service.ProductReadService;
import com.monsoon.seedflowplus.domain.product.service.ProductSimilarityService;
import com.monsoon.seedflowplus.domain.product.service.ProductWriteService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = ProductController.class,
        properties = "spring.web.resources.add-mappings=false",
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = GlobalExceptionHandler.class
        )
)
@Import(TestSecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductWriteService productWriteService;

    @MockBean
    private ProductReadService productReadService;

    @MockBean
    private ProductBookmarkService productBookmarkService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ProductSimilarityService productSimilarityService;

    // ─── GET /api/v1/products ────────────────────────────────────────────────────

    @Test
    @DisplayName("전체 상품 조회는 인증된 사용자에게 200을 반환한다")
    void getAllProducts_Returns200_ForAuthenticatedUser() throws Exception {
        ProductResponse resp = ProductResponse.builder()
                .id(1L)
                .category("WATERMELON")
                .name("수박 씨앗")
                .priceData(new ProductResponse.PriceData(100, new BigDecimal("50000"), "BOX"))
                .build();
        when(productReadService.getAllProducts(any(Role.class), any())).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/v1/products")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("수박 씨앗"));
    }

    @Test
    @DisplayName("전체 상품 조회는 비인증 요청 시 401을 반환한다")
    void getAllProducts_Returns401_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());
    }

    // ─── GET /api/v1/products/{productId} ───────────────────────────────────────

    @Test
    @DisplayName("상품 상세 조회는 인증된 사용자에게 200을 반환한다")
    void getProductDetail_Returns200() throws Exception {
        Long productId = 1L;
        ProductResponse resp = ProductResponse.builder()
                .id(productId)
                .category("WATERMELON")
                .name("수박 씨앗")
                .build();
        when(productReadService.getProductDetail(anyLong(), any(Role.class))).thenReturn(resp);

        mockMvc.perform(get("/api/v1/products/{productId}", productId)
                        .with(authentication(salesRepAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("수박 씨앗"));
    }

    // ─── DELETE /api/v1/products/{productId} ────────────────────────────────────

    @Test
    @DisplayName("상품 삭제는 인증된 사용자에게 204를 반환한다")
    void deleteProduct_Returns204() throws Exception {
        Long productId = 1L;

        mockMvc.perform(delete("/api/v1/products/{productId}", productId)
                        .with(authentication(adminAuth())))
                .andExpect(status().isNoContent());

        verify(productWriteService).deleteProduct(productId);
    }

    // ─── GET /api/v1/products/compare ───────────────────────────────────────────

    @Test
    @DisplayName("상품 비교 조회는 인증된 사용자에게 200을 반환한다")
    void getCompareProducts_Returns200() throws Exception {
        ProductResponse resp = ProductResponse.builder()
                .id(1L).category("WATERMELON").name("수박A").build();
        when(productReadService.getCompareProducts(anyList(), any(Role.class))).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/v1/products/compare")
                        .param("productIds", "1", "2")
                        .with(authentication(salesRepAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("수박A"));
    }

    // ─── GET /api/v1/products/compare/history ───────────────────────────────────

    @Test
    @DisplayName("비교 내역 히스토리 조회는 인증된 사용자에게 200을 반환한다")
    void getCompareHistories_Returns200() throws Exception {
        CompareHistoryResponse resp = CompareHistoryResponse.builder()
                .compareId(1L)
                .title("수박 비교")
                .products(List.of())
                .build();
        when(productReadService.getCompareHistories(anyLong(), any(Role.class))).thenReturn(List.of(resp));
        when(userRepository.findByLoginId(any())).thenReturn(
                java.util.Optional.of(createUserMock(5L)));

        mockMvc.perform(get("/api/v1/products/compare/history")
                        .with(authentication(salesRepAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("수박 비교"));
    }

    // ─── DELETE /api/v1/products/compare/{compareId} ────────────────────────────

    @Test
    @DisplayName("비교 내역 삭제는 인증된 사용자에게 204를 반환한다")
    void deleteCompareHistory_Returns204() throws Exception {
        Long compareId = 10L;
        when(userRepository.findByLoginId(any())).thenReturn(
                java.util.Optional.of(createUserMock(5L)));

        mockMvc.perform(delete("/api/v1/products/compare/{compareId}", compareId)
                        .with(authentication(salesRepAuth())))
                .andExpect(status().isNoContent());

        verify(productWriteService).deleteCompareHistory(anyLong(), anyLong());
    }

    // ─── GET /api/v1/products/{productId}/similar ───────────────────────────────

    @Test
    @DisplayName("유사 상품 조회는 인증된 사용자에게 200을 반환한다")
    void getSimilarProducts_Returns200() throws Exception {
        Long productId = 1L;
        SimilarProductResponse resp = SimilarProductResponse.builder()
                .productId(productId)
                .similarProducts(List.of())
                .build();
        when(productSimilarityService.getSimilarProducts(anyLong(), anyInt(), anyInt(), any()))
                .thenReturn(resp);

        mockMvc.perform(get("/api/v1/products/{productId}/similar", productId)
                        .with(authentication(salesRepAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId));
    }

    // ─── POST /api/v1/products/{productId}/bookmark ─────────────────────────────

    @Test
    @DisplayName("즐겨찾기 토글은 인증된 사용자에게 200을 반환한다")
    void toggleBookmark_Returns200() throws Exception {
        Long productId = 1L;
        doNothing().when(productBookmarkService).toggleBookmark(anyLong(), anyLong());
        when(userRepository.findByLoginId(any())).thenReturn(
                java.util.Optional.of(createUserMock(5L)));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/products/{productId}/bookmark", productId)
                        .with(authentication(salesRepAuth())))
                .andExpect(status().isOk());
    }

    // ─── GET /api/v1/products/categories ────────────────────────────────────────

    @Test
    @DisplayName("카테고리 목록 조회는 인증된 사용자에게 200을 반환하며 카테고리가 포함된다")
    void getProductCategories_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/products/categories")
                        .with(authentication(salesRepAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").exists())
                .andExpect(jsonPath("$[0].name").exists());
    }

    // ─── GET /api/v1/products/bookmarks ─────────────────────────────────────────

    @Test
    @DisplayName("즐겨찾기 목록 조회는 인증된 사용자에게 200을 반환한다")
    void getBookmarkedProducts_Returns200() throws Exception {
        when(productReadService.getBookmarkedProducts(anyLong(), any(Role.class))).thenReturn(List.of());
        when(userRepository.findByLoginId(any())).thenReturn(
                java.util.Optional.of(createUserMock(5L)));

        mockMvc.perform(get("/api/v1/products/bookmarks")
                        .with(authentication(salesRepAuth())))
                .andExpect(status().isOk());
    }

    // ─── 헬퍼 ──────────────────────────────────────────────────────────────────

    private UsernamePasswordAuthenticationToken adminAuth() {
        CustomUserDetails principal = Mockito.mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.ADMIN);
        when(principal.getUsername()).thenReturn("admin");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(principal).getAuthorities();
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private UsernamePasswordAuthenticationToken salesRepAuth() {
        CustomUserDetails principal = Mockito.mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.SALES_REP);
        when(principal.getUsername()).thenReturn("sales-rep");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_SALES_REP"))).when(principal).getAuthorities();
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private com.monsoon.seedflowplus.domain.account.entity.User createUserMock(Long id) {
        com.monsoon.seedflowplus.domain.account.entity.User user =
                com.monsoon.seedflowplus.domain.account.entity.User.builder()
                        .loginId("test-user")
                        .loginPw("pw")
                        .status(Status.ACTIVATE)
                        .role(Role.SALES_REP)
                        .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
