package gift.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SECRET_KEY = "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256";
    private static final long EXPIRATION = 3600000L; // 1시간

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Long MEMBER_ID = 5L;

    private JwtTokenProvider jwtTokenProvider;
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET_KEY, EXPIRATION);
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Test
    @DisplayName("유효한 JWT 토큰이 있으면 memberId를 request에 저장하고 필터 체인을 계속 진행한다")
    void validToken_shouldSetMemberId() throws ServletException, IOException {
        String token = jwtTokenProvider.createToken(MEMBER_ID);
        request.addHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + token);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute("memberId")).isEqualTo(MEMBER_ID);
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 필터 체인만 계속 진행된다")
    void noAuthHeader_shouldSkipProcessing() throws ServletException, IOException {
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute("memberId")).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    @DisplayName("Bearer 접두사가 없으면 필터 체인만 계속 진행된다")
    void noBearerPrefix_shouldSkipProcessing() throws ServletException, IOException {
        request.addHeader(AUTHORIZATION_HEADER, "InvalidTokenFormat");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute("memberId")).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 401 반환 후 필터 체인을 진행하지 않는다")
    void invalidToken_shouldReturn401AndStop() throws ServletException, IOException {
        String invalidToken = "this.is.not.valid.jwt.token";
        request.addHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + invalidToken);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute("memberId")).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("회원가입 경로는 필터를 적용하지 않는다")
    void registerPath_shouldSkipFilter() {
        request.setServletPath("/api/members/register");
        boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("로그인 경로는 필터를 적용하지 않는다")
    void loginPath_shouldSkipFilter() {
        request.setServletPath("/api/members/login");
        boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("루트 경로는 필터를 적용하지 않는다")
    void rootPath_shouldSkipFilter() {
        request.setServletPath("/");
        boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("일반 경로는 필터를 적용한다")
    void normalPath_shouldApplyFilter() {
        request.setServletPath("/api/products");
        boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);
        assertThat(shouldNotFilter).isFalse();
    }
}
