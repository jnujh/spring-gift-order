package gift.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.auth.JwtTokenProvider;
import gift.kakaologin.service.KakaoLoginService;
import gift.kakaomessage.service.KakaoMessageService;
import gift.member.domain.Member;
import gift.member.repository.MemberJpaRepository;
import gift.option.domain.Option;
import gift.option.dto.OptionRequest;
import gift.order.dto.OrderRequest;
import gift.product.domain.Product;
import gift.product.dto.ProductRequest;
import gift.product.service.ProductService;
import gift.wish.domain.Wish;
import gift.wish.repository.WishJpaRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberJpaRepository memberRepository;

    @Autowired
    private WishJpaRepository wishRepository;

    @Autowired
    private ProductService productService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private KakaoLoginService kakaoLoginService;

    @MockBean
    private KakaoMessageService kakaoMessageService;

    private Member testMember;
    private Option testOption;

    @BeforeEach
    void setUp() {
        testMember = memberRepository.save(Member.create("test@example.com", "password123!"));

        ProductRequest productRequest = new ProductRequest(
                "Test Product",
                1000,
                "image.jpg",
                List.of(new OptionRequest("Test Option", 10))
        );
        Product createdProduct = productService.create(productRequest);
        testOption = createdProduct.getOptions().get(0);

        when(jwtTokenProvider.validateAndParseClaims(any())).thenReturn(Jwts.claims().setSubject(String.valueOf(testMember.getId())));
        when(jwtTokenProvider.getMemberId(any(Claims.class))).thenAnswer(invocation -> {
            Claims claims = invocation.getArgument(0);
            return Long.parseLong(claims.getSubject());
        });
    }

    @Test
    @DisplayName("정상적인 주문 요청 시 201 Created를 반환한다")
    void placeOrder_Succeeds() throws Exception {
        OrderRequest request = new OrderRequest(testOption.getId(), 5, "생일 축하해!");
        String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer fake-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.optionId").value(testOption.getId()))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    @DisplayName("재고보다 많은 수량을 주문하면 409 Conflict를 반환한다")
    void placeOrder_Fails_When_InsufficientStock() throws Exception {
        OrderRequest request = new OrderRequest(testOption.getId(), 11, "재고보다 많이 주문");
        String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer fake-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("재고가 부족합니다. 현재 재고: 10"));
    }

    @Test
    @DisplayName("주문 완료 시 위시리스트에 있던 상품은 삭제된다")
    void placeOrder_RemovesProductFromWishlist() throws Exception {
        wishRepository.save(Wish.create(testMember, testOption.getProduct()));
        assertThat(wishRepository.existsByMemberAndProduct(testMember, testOption.getProduct())).isTrue();

        OrderRequest request = new OrderRequest(testOption.getId(), 1, "위시리스트 삭제 테스트");
        String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer fake-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated());

        assertThat(wishRepository.existsByMemberAndProduct(testMember, testOption.getProduct())).isFalse();
    }
}