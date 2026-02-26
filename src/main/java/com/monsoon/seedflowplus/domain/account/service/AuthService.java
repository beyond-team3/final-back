package com.monsoon.seedflowplus.domain.account.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.dto.request.LoginRequest;
import com.monsoon.seedflowplus.domain.account.dto.response.TokenResponse;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.infra.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenStore tokenStore;

    @Value("${jwt.access-token-expiration-ms:3600000}")
    private long accessTokenExpiration;

    @Transactional
    public TokenResponse login(LoginRequest request) {

        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new CoreException(ErrorType.INVALID_LOGIN));

        if (!passwordEncoder.matches(request.loginPw(), user.getLoginPw())) {
            throw new CoreException(ErrorType.INVALID_LOGIN);
        }

        if (user.getStatus() == Status.DEACTIVATE) {
            throw new CoreException(ErrorType.ACCOUNT_DISABLED);
        }

        String accessToken = jwtTokenProvider.createToken(user.getId(), user.getLoginId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getLoginId());

        tokenStore.storeRefreshToken(
                user.getLoginId(),
                refreshToken,
                Duration.ofMillis(jwtTokenProvider.getRefreshExpiration()));

        // 마지막 로그인 시간 업데이트
        user.updateLastLoginAt(LocalDateTime.now());

        return TokenResponse.of(accessToken, refreshToken, accessTokenExpiration);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        jwtTokenProvider.validateToken(refreshToken);
        String loginId = jwtTokenProvider.getLoginIdFromJWT(refreshToken);

        String savedToken = tokenStore.getRefreshToken(loginId);
        if (savedToken == null || !savedToken.equals(refreshToken)) {
            throw new IllegalStateException("유효하지 않거나 만료된 리프레시 토큰입니다.");
        }

        User user = userRepository.findByLoginId(loginId).orElse(null);
        if (user == null) {
            tokenStore.deleteRefreshToken(loginId);
            throw new CoreException(ErrorType.INVALID_LOGIN);
        }

        String newAccessToken = jwtTokenProvider.createToken(user.getId(), user.getLoginId(), user.getRole().name());
        // 리프레시 토큰도 새로 발급하여 Rotation 적용 (선택 사항이나 보안상 추천)
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getLoginId());

        tokenStore.storeRefreshToken(
                user.getLoginId(),
                newRefreshToken,
                Duration.ofMillis(jwtTokenProvider.getRefreshExpiration()));

        return TokenResponse.of(newAccessToken, newRefreshToken, accessTokenExpiration);
    }

    @Transactional
    public void logout(String refreshToken) {
        jwtTokenProvider.validateToken(refreshToken);
        String loginId = jwtTokenProvider.getLoginIdFromJWT(refreshToken);
        String savedToken = tokenStore.getRefreshToken(loginId);
        if (!refreshToken.equals(savedToken)) {
            throw new IllegalStateException("유효하지 않은 refresh 토큰");
        }
        tokenStore.deleteRefreshToken(loginId);
    }
}
