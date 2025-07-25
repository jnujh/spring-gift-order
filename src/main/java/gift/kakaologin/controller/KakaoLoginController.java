package gift.kakaologin.controller;

import gift.auth.dto.AuthResponse;
import gift.kakaologin.service.KakaoLoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class KakaoLoginController {

    private static final Logger log = LoggerFactory.getLogger(KakaoLoginController.class);

     // application.properties에 설정한 kakao.client-id와 kakao.redirect-uri 값 주입
    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    // KakaoLoginService 주입
    private final KakaoLoginService kakaoLoginService;

    public KakaoLoginController(KakaoLoginService kakaoLoginService) {
        this.kakaoLoginService = kakaoLoginService;
    }

    /**
     * 사용자가 '카카오 로그인' 버튼을 누르면 호출
     * 카카오 로그인 페이지로 리다이렉트
     * 카카오 인가 코드 요청 URL을 생성
     */
    @GetMapping("/kakao/login")
    public String startKakaoLogin() {
        String kakaoAuthUrl = UriComponentsBuilder.fromHttpUrl("https://kauth.kakao.com/oauth/authorize")
                .queryParam("scope", "talk_message,profile_nickname")
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", kakaoRedirectUri)
                .queryParam("client_id", kakaoClientId)
                .toUriString();

        log.info("Kakao Auth URL 로 Redirect: {}", kakaoAuthUrl);

        return "redirect:" + kakaoAuthUrl;
    }

    /**
     * 카카오 로그인 성공시, JWT 토큰을 JSON 으로 반환
     */
    @GetMapping("/auth/kakao/callback")
    @ResponseBody
    public ResponseEntity<AuthResponse> processKakaoLogin(@RequestParam("code") String authCode) {
        log.info("카카오 인가 코드 수신");

        String jwtToken = kakaoLoginService.processKakaoLogin(authCode);

        log.info("카카오 로그인 성공, JWT 토큰 발급 완료");

        return ResponseEntity.ok(AuthResponse.of(jwtToken));
    }

}
