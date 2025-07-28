package gift.kakaomessage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gift.kakaomessage.dto.KakaoMessageRequest;
import gift.order.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class KakaoMessageService {

    private static final Logger log = LoggerFactory.getLogger(KakaoMessageService.class);
    private static final String KAKAO_MESSAGE_API_URL = "https://kapi.kakao.com/v2/api/talk/memo/default/send";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public KakaoMessageService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // 주문 완료 카카오톡 메시지 전송
    public void sendOrderCompletionMessage(String accessToken, Order order) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        KakaoMessageRequest kakaoRequest = KakaoMessageRequest.from(order);

        try {
            String templateObjectJson = objectMapper.writeValueAsString(kakaoRequest.templateObject());

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("template_object", templateObjectJson);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(KAKAO_MESSAGE_API_URL, request, String.class);
            log.info("주문 완료 카카오톡 메시지 전송 성공. 받는사람: {}", order.getMember().getEmail());

        } catch (JsonProcessingException e) {
            log.error("카카오 메시지 template_object JSON 변환 실패", e);
        } catch (HttpClientErrorException e) {
            log.error("카카오 메시지 API 호출 실패. Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
        }
    }
}