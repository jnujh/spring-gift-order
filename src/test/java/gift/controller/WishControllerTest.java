package gift.controller;

import gift.auth.JwtTokenProvider;
import gift.kakaologin.service.KakaoLoginService;
import gift.kakaomessage.service.KakaoMessageService;
import gift.member.domain.Member;
import gift.member.repository.MemberJpaRepository;
import gift.option.dto.OptionRequest;
import gift.product.domain.Product;
import gift.product.dto.ProductRequest;
import gift.product.service.ProductService;
import gift.wish.dto.WishResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class WishControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    ProductService productService;

    @Autowired
    MemberJpaRepository memberRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private KakaoLoginService kakaoLoginService;
    @MockBean
    private KakaoMessageService kakaoMessageService;

    private String baseUrl;
    private RestTemplate restTemplate;
    private Member testMember1;
    private Member testMember2;

    @PostConstruct
    void setupRestTemplate() {
        this.restTemplate = new RestTemplate();
    }

    @BeforeEach
    void setUp() {
        this.baseUrl = "http://localhost:" + port + "/api/wishes";
        this.testMember1 = memberRepository.save(Member.create("user1@example.com", "password123!"));
        this.testMember2 = memberRepository.save(Member.create("user2@example.com", "password123!"));

        when(jwtTokenProvider.validateAndParseClaims(anyString())).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            if ("token-for-user-2".equals(token)) {
                return Jwts.claims().setSubject(String.valueOf(testMember2.getId()));
            }
            return Jwts.claims().setSubject(String.valueOf(testMember1.getId()));
        });

        when(jwtTokenProvider.getMemberId(any(Claims.class))).thenAnswer(invocation -> {
            Claims claims = invocation.getArgument(0);
            return Long.parseLong(claims.getSubject());
        });
    }

    @Test
    @DisplayName("상품을 찜할 수 있다")
    void addWish() {
        String token = "token-for-user-1";
        Product product = createTestProduct("테스트상품", 1000);

        HttpHeaders headers = authHeader(token);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                Map.of("productId", product.getId()), headers);

        ResponseEntity<WishResponse> response = restTemplate.postForEntity(baseUrl, request, WishResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().productId()).isEqualTo(product.getId());
    }

    @Test
    @DisplayName("같은 상품을 중복 찜하면 409 Conflict가 발생한다")
    void duplicateWish() {
        String token = "token-for-user-1";
        Product product = createTestProduct("중복상품", 500);

        HttpHeaders headers = authHeader(token);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                Map.of("productId", product.getId()), headers);

        restTemplate.postForEntity(baseUrl, request, WishResponse.class);

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.Conflict.class, () -> {
            restTemplate.postForEntity(baseUrl, request, String.class);
        });

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getResponseBodyAsString()).contains("이미 찜한 상품입니다.");
    }

    @Test
    @DisplayName("찜한 상품을 위시리스트에서 조회할 수 있다")
    void getWishlist() {
        String token = "token-for-user-1";
        Product product = createTestProduct("조회상품", 700);

        HttpHeaders headers = authHeader(token);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                Map.of("productId", product.getId()), headers);

        restTemplate.postForEntity(baseUrl, request, WishResponse.class);

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                getRequest,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"productId\":" + product.getId());
    }


    @Test
    @DisplayName("찜 항목을 삭제할 수 있다 (멱등성 보장)")
    void removeWish() {
        String token = "token-for-user-1";
        Product product = createTestProduct("삭제상품", 900);

        HttpHeaders headers = authHeader(token);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                Map.of("productId", product.getId()), headers);
        ResponseEntity<WishResponse> addRes = restTemplate.postForEntity(baseUrl, request, WishResponse.class);

        Long wishId = addRes.getBody().wishId();
        HttpEntity<Void> deleteRequest = new HttpEntity<>(headers);
        ResponseEntity<Void> deleteRes = restTemplate.exchange(
                baseUrl + "/" + wishId, HttpMethod.DELETE, deleteRequest, Void.class);

        assertThat(deleteRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Void> secondDelete = restTemplate.exchange(
                baseUrl + "/" + wishId, HttpMethod.DELETE, deleteRequest, Void.class);

        assertThat(secondDelete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("다른 사용자의 찜 항목을 삭제하면 403 Forbidden 발생")
    void removeOthersWish() {
        String token1 = "token-for-user-1";
        String token2 = "token-for-user-2";
        Product product = createTestProduct("타인상품", 1100);

        HttpHeaders headers1 = authHeader(token1);
        HttpEntity<Map<String, Object>> req1 = new HttpEntity<>(
                Map.of("productId", product.getId()), headers1);
        ResponseEntity<WishResponse> response = restTemplate.postForEntity(baseUrl, req1, WishResponse.class);
        Long wishId = response.getBody().wishId();

        HttpHeaders headers2 = authHeader(token2);
        HttpEntity<Void> deleteRequest = new HttpEntity<>(headers2);

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.Forbidden.class, () -> {
            restTemplate.exchange(baseUrl + "/" + wishId, HttpMethod.DELETE, deleteRequest, String.class);
        });

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getResponseBodyAsString()).contains("다른 사용자의 위시리스트 항목은 삭제할 수 없습니다.");
    }

    private Product createTestProduct(String name, int price) {
        ProductRequest request = new ProductRequest(
                name,
                price,
                "http://img.com/" + name + ".jpg",
                List.of(new OptionRequest("기본", 10))
        );
        return productService.create(request);
    }

    private HttpHeaders authHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}