package gift.kakaologin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import gift.auth.JwtTokenProvider;
import gift.kakaologin.exception.InvalidKakaoAuthCodeException;
import gift.kakaologin.exception.MismatchedKakaoRedirectUriException;
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

        // 멤버를 찾거나 생성 및 액세스 토큰 업데이트
        Member member = findOrCreateMember(userInfo);
        member.updateKakaoAccessToken(accessToken);

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
        } catch (HttpClientErrorException e) {
            // HTTP 상태 코드 확인
            log.error("카카오 API 요청 실패. Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) { // 400 Bad Request
                try {
                    // JSON 본문 파싱 - 에러 코드 확인
                    JsonNode errorNode = objectMapper.readTree(e.getResponseBodyAsString());
                    String errorCode = errorNode.path("error_code").asText();

                    // 에러 코드에 대응하는 전용 예외 던진다.
                    switch (errorCode) {
                        case "KOE320":
                            throw new InvalidKakaoAuthCodeException("유효하지 않은 카카오 인가 코드입니다.", e);
                        case "KOE303":
                            throw new MismatchedKakaoRedirectUriException("설정된 Redirect URI와 일치하지 않습니다.", e);
                        default:
                            // 다른 400 에러들
                            throw new RuntimeException("카카오 토큰 요청 중 Bad Request 에러가 발생했습니다.", e);
                    }
                } catch (JsonProcessingException jsonEx) {
                    // 에러 응답 본문을 파싱하지 못하는 경우
                    throw new RuntimeException("카카오 에러 응답을 파싱하는데 실패했습니다.", jsonEx);
                }
            }
            // 400 외 다른 HTTP 에러
            throw new RuntimeException("카카오 토큰 요청에 실패했습니다.", e);

        } catch (JsonProcessingException e) {
            // 성공 응답이 왔지만, JSON 형식이 아닐 경우
            log.error("카카오 응답 파싱 실패", e);
            throw new RuntimeException("카카오 응답을 파싱하는데 실패했습니다.", e);
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
