package gift.auth;

import io.jsonwebtoken.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String secretKey;
    private String differentKey;

    @BeforeEach
    void setUp() {
        secretKey = "test-secret-key-for-jwt-token-provider-unit-test-must-be-at-least-256-bits";
        differentKey = "different-secret-key-that-is-long-enough-for-jwt-requirements-256-bits";
        long expirationTime = 3600000; // 1시간

        jwtTokenProvider = new JwtTokenProvider(secretKey, expirationTime);
    }

    @Test
    @DisplayName("회원 ID로 JWT 토큰을 생성한다")
    void createToken_success() {
        Long memberId = 5L;
        String token = jwtTokenProvider.createToken(memberId);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("정상 토큰에서 회원 ID를 추출할 수 있다")
    void getMemberId_success() {
        Long expectedMemberId = 5L;
        String token = jwtTokenProvider.createToken(expectedMemberId);
        Claims claims = jwtTokenProvider.validateAndParseClaims(token);

        Long actualMemberId = jwtTokenProvider.getMemberId(claims);

        assertThat(actualMemberId).isEqualTo(expectedMemberId);
    }

    @Test
    @DisplayName("만료된 토큰은 ExpiredJwtException을 발생시킨다")
    void expiredToken_throwsExpiredJwtException() throws InterruptedException {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(secretKey, 1);
        String token = shortLivedProvider.createToken(1L);
        Thread.sleep(10);

        assertThatThrownBy(() -> shortLivedProvider.validateAndParseClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "this.is.not.jwt",
            "123", // 단순 숫자
            "abc.def", // dot 1개
            "header.payload.signature.extra", // dot 3개 이상
    })
    @DisplayName("형식이 잘못된 토큰은 MalformedJwtException을 발생시킨다")
    void malformedToken_throwsMalformedJwtException(String malformedToken) {
        assertThatThrownBy(() -> jwtTokenProvider.validateAndParseClaims(malformedToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("서명이 잘못된 토큰은 SignatureException을 발생시킨다")
    void signatureMismatch_throwsSignatureException() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(differentKey, 3600000);
        String token = otherProvider.createToken(5L);

        assertThatThrownBy(() -> jwtTokenProvider.validateAndParseClaims(token))
                .isInstanceOf(SignatureException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null 또는 빈 토큰은 IllegalArgumentException을 발생시킨다")
    void nullOrEmptyToken_throwsIllegalArgumentException(String token) {
        assertThatThrownBy(() -> jwtTokenProvider.validateAndParseClaims(token))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
