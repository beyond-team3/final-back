package com.monsoon.seedflowplus.domain.account.service;

import com.monsoon.seedflowplus.domain.account.dto.request.LoginRequest;
import com.monsoon.seedflowplus.domain.account.dto.response.TokenResponse;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.infra.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

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

        if (request.loginId() == null || request.loginId().isEmpty() ||
                request.loginPw() == null || request.loginPw().isEmpty()) {
            throw new BadCredentialsException("아이디 또는 비밀번호가 null값입니다.");
        }

        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BadCredentialsException("아이디 또는 비밀번호가 잘못 되었습니다."));

        if (!passwordEncoder.matches(request.loginPw(), user.getLoginPw())) {
            throw new BadCredentialsException("아이디 또는 비밀번호가 잘못 되었습니다.");
        }

        String accessToken = jwtTokenProvider.createToken(user.getId(), user.getLoginId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getLoginId());

        tokenStore.storeRefreshToken(
                user.getLoginId(),
                refreshToken,
                Duration.ofMillis(jwtTokenProvider.getRefreshExpiration()));

        return TokenResponse.of(accessToken, refreshToken, accessTokenExpiration);
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
