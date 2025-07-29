package gift.kakaologin;

import gift.auth.JwtTokenProvider;
import gift.global.exception.GlobalExceptionHandler;
import gift.kakaologin.controller.KakaoLoginController;
import gift.kakaologin.service.KakaoLoginService;
import gift.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = KakaoLoginController.class)
@Import(GlobalExceptionHandler.class)
class KakaoLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KakaoLoginService kakaoLoginService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private MemberService memberService;

    @Test
    @DisplayName("/kakao/login 요청 시 Mock 서비스가 제공하는 URL로 리다이렉트한다")
    void startKakaoLogin_shouldRedirectToUrlFromService() throws Exception {
        String expectedKakaoAuthUrl = "https://fake-kakao-auth.com/login";
        when(kakaoLoginService.getKakaoAuthUrl()).thenReturn(expectedKakaoAuthUrl);

        mockMvc.perform(get("/kakao/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", expectedKakaoAuthUrl));
    }

    @Test
    @DisplayName("성공 인가 코드로 콜백 요청 시 JWT 토큰을 JSON으로 반환한다")
    void processKakaoLogin_shouldReturnJwt() throws Exception {
        String successCode = "success-code";
        String expectedJwt = "fake-jwt-token-from-kakao-login";
        when(kakaoLoginService.processKakaoLogin(successCode)).thenReturn(expectedJwt);

        mockMvc.perform(get("/auth/kakao/callback").param("code", successCode))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value(expectedJwt));
    }

    @Test
    @DisplayName("실패 인가 코드로 콜백 요청 시 400 Bad Request를 반환한다")
    void processKakaoLogin_whenInvalidAuthCode_shouldReturnBadRequest() throws Exception {
        String failCode = "fail-code";
        when(kakaoLoginService.processKakaoLogin(failCode))
                .thenThrow(new IllegalArgumentException("카카오 로그인에 실패했습니다. 다시 시도해주세요."));

        mockMvc.perform(get("/auth/kakao/callback").param("code", failCode))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("카카오 로그인에 실패했습니다. 다시 시도해주세요."));
    }
}