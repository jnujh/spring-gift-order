package gift.kakaologin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gift.auth.JwtTokenProvider;
import gift.kakaologin.exception.InvalidKakaoAuthCodeException;
import gift.kakaologin.exception.MismatchedKakaoRedirectUriException;
import gift.member.domain.Member;
import gift.kakaologin.service.KakaoLoginService;
import gift.member.repository.MemberJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KakaoLoginServiceTest {

    @InjectMocks
    private KakaoLoginService kakaoLoginService;

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private MemberJpaRepository memberRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private final String authCode = "test_auth_code";
    private final String accessToken = "test_access_token";
    private final String fakeJwt = "fake.jwt.token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kakaoLoginService, "kakaoClientId", "test-client-id");
        ReflectionTestUtils.setField(kakaoLoginService, "kakaoRedirectUri", "http://localhost/callback");
    }

    private void mockKakaoApiSuccess() throws JsonProcessingException {
        // Mocking: 액세스 토큰 발급 성공 과정
        String tokenResponseJson = "{\"access_token\":\"" + accessToken + "\"}";
        JsonNode tokenNode = new ObjectMapper().readTree(tokenResponseJson);
        given(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .willReturn(ResponseEntity.ok(tokenResponseJson));
        given(objectMapper.readTree(tokenResponseJson)).willReturn(tokenNode);

        // Mocking: 사용자 정보 조회 성공 과정
        long socialId = 12345L;
        String nickname = "테스트유저";
        String userInfoResponseJson = "{\"id\":" + socialId + ", \"properties\":{\"nickname\":\"" + nickname + "\"}}";
        JsonNode userInfoNode = new ObjectMapper().readTree(userInfoResponseJson);
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .willReturn(ResponseEntity.ok(userInfoResponseJson));
        given(objectMapper.readTree(userInfoResponseJson)).willReturn(userInfoNode);
    }

    @Test
    @DisplayName("신규 회원이 카카오 로그인을 하면, 회원가입 후 JWT를 반환한다")
    void processKakaoLogin_whenNewUser_thenRegisterAndReturnJwt() throws JsonProcessingException {
        // given
        mockKakaoApiSuccess(); // 카카오 API 연동 성공 시나리오 Mocking
        given(memberRepository.findBySocialId(anyLong())).willReturn(Optional.empty());
        Member newMember = Member.createSocial("kakao_12345@social.com", "password", 12345L);
        given(memberRepository.save(any(Member.class))).willReturn(newMember);
        given(jwtTokenProvider.createToken(any())).willReturn(fakeJwt);

        // when
        String resultToken = kakaoLoginService.processKakaoLogin(authCode);

        // then
        verify(memberRepository).save(any(Member.class));
        assertThat(resultToken).isEqualTo(fakeJwt);
    }

    @Test
    @DisplayName("기존 회원이 카카오 로그인을 하면, 회원가입 없이 JWT를 반환한다")
    void processKakaoLogin_whenExistingUser_thenReturnJwt() throws JsonProcessingException {
        // given
        mockKakaoApiSuccess(); // 카카오 API 연동 성공 시나리오 Mocking
        Member existingMember = Member.createSocial("kakao_12345@social.com", "password", 12345L);
        given(memberRepository.findBySocialId(anyLong())).willReturn(Optional.of(existingMember));
        given(jwtTokenProvider.createToken(any())).willReturn(fakeJwt);

        // when
        String resultToken = kakaoLoginService.processKakaoLogin(authCode);

        // then
        verify(memberRepository, never()).save(any(Member.class));
        assertThat(resultToken).isEqualTo(fakeJwt);
    }

    @Test
    @DisplayName("KOE320 에러(잘못된 인가 코드) 발생 시 InvalidKakaoAuthCodeException을 던진다")
    void getAccessToken_whenInvalidAuthCode_thenThrowCustomException() throws JsonProcessingException {
        // given: 카카오 API가 KOE320 에러를 반환하는 상황을 Mocking
        String errorJson = "{\"error_code\":\"KOE320\"}";
        JsonNode errorNode = new ObjectMapper().readTree(errorJson);

        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request");
        given(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .willThrow(exception);
        // 에러 응답 본문을 파싱하는 objectMapper도 Mocking
        given(objectMapper.readTree(exception.getResponseBodyAsString())).willReturn(errorNode);

        // when & then
        assertThatThrownBy(() -> kakaoLoginService.processKakaoLogin(authCode))
                .isInstanceOf(InvalidKakaoAuthCodeException.class)
                .hasMessageContaining("유효하지 않은 카카오 인가 코드입니다.");
    }

    @Test
    @DisplayName("KOE303 에러(Redirect URI 불일치) 발생 시 MismatchedKakaoRedirectUriException을 던진다")
    void getAccessToken_whenMismatchedRedirectUri_thenThrowCustomException() throws JsonProcessingException {
        // given: 카카오 API가 KOE303 에러를 반환하는 상황을 Mocking
        String errorJson = "{\"error_code\":\"KOE303\"}";
        JsonNode errorNode = new ObjectMapper().readTree(errorJson);

        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request");
        given(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .willThrow(exception);
        given(objectMapper.readTree(exception.getResponseBodyAsString())).willReturn(errorNode);

        // when & then
        assertThatThrownBy(() -> kakaoLoginService.processKakaoLogin(authCode))
                .isInstanceOf(MismatchedKakaoRedirectUriException.class)
                .hasMessageContaining("설정된 Redirect URI와 일치하지 않습니다.");
    }


    @Test
    @DisplayName("예상치 못한 HTTP 에러(400 외) 발생 시 RuntimeException을 던진다") // 테스트 의도를 더 명확하게 변경
    void getAccessToken_whenUnexpectedHttpError_thenThrowRuntimeException() {
        // given: RestTemplate이 400이 아닌 다른 HTTP 에러를 던지도록 설정 (ex 401 Unauthorized)
        given(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .willThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "인증 실패"));

        // when & then
        assertThatThrownBy(() -> kakaoLoginService.processKakaoLogin(authCode))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("카카오 토큰 요청에 실패했습니다.");
    }

    @Test
    @DisplayName("카카오 사용자 정보 요청이 실패하면 RuntimeException을 던진다")
    void getKakaoUserInfo_whenKakaoFails_thenThrowException() throws JsonProcessingException {
        // given
        // 1. 액세스 토큰 요청은 성공
        String tokenResponseJson = "{\"access_token\":\"" + accessToken + "\"}";
        JsonNode tokenNode = new ObjectMapper().readTree(tokenResponseJson);
        given(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .willReturn(ResponseEntity.ok(tokenResponseJson));
        given(objectMapper.readTree(tokenResponseJson)).willReturn(tokenNode);

        // 2. 하지만 사용자 정보 요청은 실패
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .willThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "잘못된 토큰"));

        // when & then
        assertThatThrownBy(() -> kakaoLoginService.processKakaoLogin(authCode))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("카카오 사용자 정보 조회에 실패했습니다.");
    }
}