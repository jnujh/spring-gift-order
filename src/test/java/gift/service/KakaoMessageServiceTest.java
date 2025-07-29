package gift.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gift.kakaomessage.exception.KakaoMessageException;
import gift.kakaomessage.service.KakaoMessageService;
import gift.member.domain.Member;
import gift.option.domain.Option;
import gift.option.dto.OptionRequest;
import gift.order.domain.Order;
import gift.product.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KakaoMessageServiceTest {

    @InjectMocks
    private KakaoMessageService kakaoMessageService;

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private Member testMember;
    private Order testOrder;
    private static final String KAKAO_MESSAGE_API_URL = "https://kapi.kakao.com/v2/api/talk/memo/default/send";

    @BeforeEach
    void setUp() {
        testMember = Member.create("test@example.com", "password123!");
        Product testProduct = Product.create(
                "Test Product", 1000, "image.jpg",
                List.of(new OptionRequest("Test Option", 10))
        );
        Option testOption = testProduct.getOptions().get(0);
        testOrder = Order.create(testMember, testOption, 1, "Test Message");
    }

    @Test
    @DisplayName("카카오 메시지 전송에 성공한다")
    void sendOrderCompletionMessage_Succeeds() {
        // given
        String accessToken = "fake-access-token";
        ResponseEntity<String> successResponse = new ResponseEntity<>("{\"result_code\":0}", HttpStatus.OK);
        when(restTemplate.postForEntity(eq(KAKAO_MESSAGE_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(successResponse);

        // when
        kakaoMessageService.sendOrderCompletionMessage(accessToken, testOrder);

        // then
        ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(KAKAO_MESSAGE_API_URL), captor.capture(), eq(String.class));

        HttpEntity<MultiValueMap<String, String>> capturedRequest = captor.getValue();
        assertThat(capturedRequest.getHeaders().getFirst("Authorization")).isEqualTo("Bearer " + accessToken);

        MultiValueMap<String, String> body = capturedRequest.getBody();
        String templateJson = body.getFirst("template_object");
        assertThat(templateJson).isNotNull();
        assertThat(templateJson).contains("\"object_type\":\"text\"");
        assertThat(templateJson).contains(testOrder.getOption().getProduct().getName());
    }

    @Test
    @DisplayName("카카오 API 호출이 실패하면 KakaoMessageException을 던진다")
    void sendOrderCompletionMessage_Fails_When_ApiCallFails() {
        // given
        String accessToken = "invalid-access-token";
        HttpClientErrorException apiException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        when(restTemplate.postForEntity(eq(KAKAO_MESSAGE_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenThrow(apiException);

        // when & then
        assertThatThrownBy(() -> kakaoMessageService.sendOrderCompletionMessage(accessToken, testOrder))
                .isInstanceOf(KakaoMessageException.class)
                .hasMessageContaining("카카오 메시지 전송에 실패했습니다.")
                .hasCause(apiException);
    }

    @Test
    @DisplayName("JSON 변환에 실패하면 KakaoMessageException을 던진다")
    void sendOrderCompletionMessage_Fails_When_JsonProcessingFails() throws JsonProcessingException {
        // given
        String accessToken = "fake-access-token";
        ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
        KakaoMessageService serviceWithMockMapper = new KakaoMessageService(restTemplate, mockObjectMapper);

        when(mockObjectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("json error") {});

        // when & then
        assertThatThrownBy(() -> serviceWithMockMapper.sendOrderCompletionMessage(accessToken, testOrder))
                .isInstanceOf(KakaoMessageException.class)
                .hasMessageContaining("카카오 메시지 요청을 만드는 데 실패했습니다.")
                .hasCauseInstanceOf(JsonProcessingException.class);
    }
}