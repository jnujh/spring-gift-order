package gift.service;

import gift.auth.JwtTokenProvider;
import gift.kakaologin.service.KakaoLoginService;
import gift.kakaomessage.service.KakaoMessageService;
import gift.product.domain.Product;
import gift.option.dto.OptionRequest;
import gift.product.dto.ProductRequest;
import gift.product.exception.ProductNotFoundException;
import gift.product.repository.ProductJpaRepository;
import gift.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProductServiceTest {

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private ProductService productService;

    @MockBean
    private KakaoLoginService kakaoLoginService;

    @MockBean
    private KakaoMessageService kakaoMessageService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("옵션이 없으면 상품 생성에 실패한다")
    void createProduct_Fails_When_NoOptions() {
        ProductRequest request = new ProductRequest(
                "옵션없는 상품",
                15000,
                "http://img.jpg",
                List.of()
        );

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품은 최소 하나의 옵션을 포함해야 합니다.");
    }

    @Test
    @DisplayName("상품이 정상적으로 등록된다")
    void createProduct_Succeeds() {
        ProductRequest request = new ProductRequest(
                "초코파이",
                1000,
                "http://example.com/chocopie.jpg",
                List.of(new OptionRequest("기본", 10))
        );
        Product result = productService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("초코파이");
        assertThat(result.getOptions()).hasSize(1);
    }

    @Test
    @DisplayName("상품명에 '카카오'가 포함되면 예외가 발생한다")
    void createProduct_Fails_When_NameContainsKakao() {
        ProductRequest request = new ProductRequest(
                "카카오프렌즈 에디션",
                1000,
                "http://example.com/kakao.jpg",
                List.of(new OptionRequest("기본", 10))
        );
        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'카카오'가 포함된 상품명은 MD와 협의 후 등록 가능합니다.");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 수정하면 예외가 발생한다")
    void updateProduct_Fails_When_IdNotFound() {
        Long invalidId = 9999L;

        assertThatThrownBy(() -> productService.update(invalidId, "새이름", 100, "url"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("상품명에 '카카오'가 포함되면 업데이트에서도 예외가 발생한다")
    void updateProduct_Fails_When_NameContainsKakao() {
        ProductRequest request = new ProductRequest(
                "초코파이",
                1000,
                "http://example.com/chocopie.jpg",
                List.of(new OptionRequest("기본", 10))
        );
        Product saved = productService.create(request);

        assertThatThrownBy(() -> productService.update(saved.getId(), "카카오 초코송이", 1000, "http://img.jpg"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("상품이 정상적으로 수정된다")
    void updateProduct_Succeeds() {
        ProductRequest request = new ProductRequest(
                "몽쉘",
                1500,
                "http://img.jpg",
                List.of(new OptionRequest("기본", 10))
        );
        Product savedProduct = productService.create(request);

        productService.update(savedProduct.getId(), "매운 새우깡", 1300, "http://img2.jpg");

        Product updatedProduct = productJpaRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getName()).isEqualTo("매운 새우깡");
        assertThat(updatedProduct.getPrice()).isEqualTo(1300);
        assertThat(updatedProduct.getImageUrl()).isEqualTo("http://img2.jpg");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 삭제해도 예외가 발생하지 않는다 (멱등성)")
    void deleteProduct_DoesNotThrow_When_IdNotFound() {
        Long invalidId = 9999L;

        assertDoesNotThrow(() -> productService.delete(invalidId));
    }
}