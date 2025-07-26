package gift.kakaologin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import gift.auth.JwtTokenProvider;
import gift.member.domain.Member;
import gift.kakaologin.dto.KakaoUserInfo;
import gift.member.repository.MemberJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Service
public class KakaoLoginService {

    private static final Logger log = LoggerFactory.getLogger(KakaoLoginService.class);

    // application.properties에 설정한 kakao.client-id와 kakao.redirect-uri 값 주입
    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MemberJpaRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public KakaoLoginService(RestTemplate restTemplate, ObjectMapper objectMapper, MemberJpaRepository memberRepository, JwtTokenProvider jwtTokenProvider) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.memberRepository = memberRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 카카오 인가 코드 요청 URL을 생성
    public String getKakaoAuthUrl() {
        String kakaoAuthUrl = UriComponentsBuilder.fromHttpUrl("https://kauth.kakao.com/oauth/authorize")
                .queryParam("scope", "talk_message,profile_nickname")
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", kakaoRedirectUri)
                .queryParam("client_id", kakaoClientId)
                .toUriString();

        return kakaoAuthUrl;
    }

    @Transactional
    public String processKakaoLogin(String authCode) {
        String accessToken = getAccessToken(authCode);
        KakaoUserInfo userInfo = getKakaoUserInfo(accessToken);
        Member member = findOrCreateMember(userInfo);
        return jwtTokenProvider.createToken(member.getId());
    }

    // 1. 인가 코드로 카카오 액세스 토큰 발급 요청
    private String getAccessToken(String authCode) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakaoClientId);
        body.add("redirect_uri", kakaoRedirectUri);
        body.add("code", authCode);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            String accessToken = rootNode.path("access_token").asText();
            log.info("카카오 액세스 토큰 발급 성공");
            return accessToken;
        } catch (HttpClientErrorException | JsonProcessingException e) {
            log.error("카카오 액세스 토큰 발급 실패", e);
            throw new RuntimeException("카카오 토큰 요청에 실패했습니다.", e);
        }
    }

    // 2. 액세스 토큰으로 카카오 사용자 정보(고유 ID, 닉네임) 요청
    private KakaoUserInfo getKakaoUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, String.class);
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            long id = rootNode.path("id").asLong();
            String nickname = rootNode.path("properties").path("nickname").asText();
            log.info("카카오 사용자 정보 조회 성공. 고유 ID: {}, 닉네임: {}", id, nickname);
            return new KakaoUserInfo(id, nickname);
        } catch (HttpClientErrorException | JsonProcessingException e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            throw new RuntimeException("카카오 사용자 정보 조회에 실패했습니다.", e);
        }
    }

    // 3. 카카오 정보로 회원 조회 또는 자동 가입
    private Member findOrCreateMember(KakaoUserInfo userInfo) {
        return memberRepository.findBySocialId(userInfo.id())
                .orElseGet(() -> {
                    log.info("신규 회원. 회원가입 진행: {}", userInfo.nickname());
                    // 소셜 로그인은 이메일/비밀번호 사용 X, 임의로 생성
                    String virtualEmail = "kakao_" + userInfo.id() + "@social.com";
                    String randomPassword = UUID.randomUUID().toString();

                    Member newMember = Member.createSocial(virtualEmail, randomPassword, userInfo.id());
                    return memberRepository.save(newMember);
                });
    }

}
