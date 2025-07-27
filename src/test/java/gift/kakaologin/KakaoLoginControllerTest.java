package gift.kakaologin;

import gift.auth.JwtTokenProvider;
import gift.global.exception.GlobalExceptionHandler;
import gift.kakaologin.controller.KakaoLoginController;
import gift.kakaologin.exception.InvalidKakaoAuthCodeException;
import gift.kakaologin.exception.MismatchedKakaoRedirectUriException;
import gift.kakaologin.service.KakaoLoginService;
import gift.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(GlobalExceptionHandler.class)
@WebMvcTest(KakaoLoginController.class)
@TestPropertySource(properties = {
        "kakao.client-id=test-client-id",
        "kakao.redirect-uri=http://localhost:8080/auth/kakao/callback"
})
class KakaoLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private KakaoLoginService kakaoLoginService;

    @MockBean
    private MemberService memberService;

    @Test
    @DisplayName("/kakao/login 요청 시 서비스가 제공하는 URL로 리다이렉트한다")
    void startKakaoLogin_shouldRedirectToUrlFromService() throws Exception {
        // given: 서비스가 특정 URL을 반환하도록 미리 정의
        String expectedKakaoAuthUrl = "https://kauth.kakao.com/oauth/authorize?some_params_from_service";
        given(kakaoLoginService.getKakaoAuthUrl()).willReturn(expectedKakaoAuthUrl);

        // when & then
        mockMvc.perform(get("/kakao/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", expectedKakaoAuthUrl)); // 서비스가 반환한 URL과 정확히 일치하는지 검증
    }

    @Test
    @DisplayName("카카오 로그인 콜백 요청 시 JWT 토큰을 JSON으로 반환한다")
    void processKakaoLogin_shouldReturnJwt() throws Exception {
        // given
        String authCode = "test_auth_code";
        String fakeJwt = "fake.jwt.token";
        given(kakaoLoginService.processKakaoLogin(authCode)).willReturn(fakeJwt);

        // when & then
        mockMvc.perform(get("/auth/kakao/callback").param("code", authCode))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value(fakeJwt));
    }

    @Test
    @DisplayName("서비스에서 InvalidKakaoAuthCodeException 발생 시 400 Bad Request를 반환한다")
    void processKakaoLogin_whenInvalidAuthCode_shouldReturnBadRequest() throws Exception {
        // given
        String authCode = "invalid_auth_code";
        // 서비스가 특정 예외를 던지도록 Mocking
        willThrow(new InvalidKakaoAuthCodeException("유효하지 않은 코드", null))
                .given(kakaoLoginService).processKakaoLogin(authCode);

        // when & then
        mockMvc.perform(get("/auth/kakao/callback").param("code", authCode))
                .andExpect(status().isBadRequest()) // 400 상태 코드를 기대
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("카카오 로그인에 실패했습니다. 다시 시도해주세요."));
    }

    @Test
    @DisplayName("서비스에서 MismatchedKakaoRedirectUriException 발생 시 500 Internal Server Error를 반환한다")
    void processKakaoLogin_whenMismatchedRedirectUri_shouldReturnInternalServerError() throws Exception {
        // given
        String authCode = "any_auth_code";
        // 서비스가 특정 예외를 던지도록 Mocking
        willThrow(new MismatchedKakaoRedirectUriException("URI 불일치", null))
                .given(kakaoLoginService).processKakaoLogin(authCode);

        // when & then
        mockMvc.perform(get("/auth/kakao/callback").param("code", authCode))
                .andExpect(status().isInternalServerError()) // 500 상태 코드를 기대
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("서버에 오류가 발생했습니다. 관리자에게 문의하세요."));
    }
}