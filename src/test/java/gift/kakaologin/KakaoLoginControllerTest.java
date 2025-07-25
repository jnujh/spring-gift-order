package gift.kakaologin;

import gift.auth.JwtTokenProvider;
import gift.kakaologin.controller.KakaoLoginController;
import gift.kakaologin.service.KakaoLoginService;
import gift.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    @DisplayName("/kakao/login 요청 시 카카오 인증 URL로 리다이렉트한다")
    void startKakaoLogin_shouldRedirectToKakao() throws Exception {
        mockMvc.perform(get("/kakao/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", allOf(
                        containsString("kauth.kakao.com/oauth/authorize"),
                        containsString("client_id=test-client-id"),
                        containsString("redirect_uri=http://localhost:8080/auth/kakao/callback")
                )));
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
}