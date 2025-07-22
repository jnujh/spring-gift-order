package gift.service;

import gift.domain.Product;
import gift.dto.OptionRequest;
import gift.dto.ProductRequest;
import gift.exception.ProductNotFoundException;
import gift.repository.ProductRepository;
import gift.domain.ProductOld;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ProductServiceTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Test
    void 옵션이_없으면_상품_생성에_실패한다() {
        // given
        ProductRequest request = new ProductRequest(
                "옵션없는 상품",
                15000,
                "http://img.jpg",
                List.of() // 옵션 없음
        );

        // when / then
        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품은 최소 하나의 옵션을 포함해야 합니다.");
    }

    @Test
    void 상품이_정상적으로_등록된다() {
        // given
        ProductRequest request = new ProductRequest(
                "초코파이",
                1000,
                "http://example.com/chocopie.jpg",
                List.of(new OptionRequest("기본", 10))
        );
        // when
        Product result = productService.create(request);

        // then
        assertNotNull(result);
        assertEquals("초코파이", result.getName());
    }

    @Test
    void 상품명에_카카오가_포함되면_예외가_발생한다() {
        // given
        ProductRequest request = new ProductRequest(
                "카카오",
                1000,
                "http://example.com/kakao.jpg",
                List.of(new OptionRequest("기본", 10))
        );
        // when & then
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> productService.create(request)
        );

        assertEquals("'카카오'가 포함된 상품명은 MD와 협의 후 등록 가능합니다.", e.getMessage());
    }

    @Test
    void 존재하지_않는_ID로_수정하면_예외가_발생한다() {
        // given
        Long invalidId = 999L;
        String name = "새로운 이름";
        int price = 3000;
        String imageUrl = "http://example.com/image.jpg";

        // when & then
        assertThrows(ProductNotFoundException.class, () ->
                productService.update(invalidId, name, price, imageUrl));
    }

    @Test
    void 상품명에_카카오가_포함되면_업데이트에서도_예외가_발생한다() {
        // given
        ProductRequest request = new ProductRequest(
                "초코파이",
                1000,
                "http://example.com/chocopie.jpg",
                List.of(new OptionRequest("기본", 10))
        );

        Product saved = productService.create(request);

        // when & then
        assertThrows(IllegalArgumentException.class, () ->
                productService.update(saved.getId(), "카카오 초코송이", 1000, "http://img.jpg"));
    }

    @Test
    void 수정사항이_없어도_예외없이_정상처리된다() {
        // given
        ProductRequest request = new ProductRequest(
                "몽쉘",
                1500,
                "http://img.jpg",
                List.of(new OptionRequest("기본", 10))
        );
        Product saved = productService.create(request);

        // when & then
        assertDoesNotThrow(() ->
                productService.update(saved.getId(), "몽쉘", 1500, "http://img.jpg"));
    }

    @Test
    void 상품이_정상적으로_수정된다() {
        // given
        ProductRequest request = new ProductRequest(
                "몽쉘",
                1500,
                "http://img.jpg",
                List.of(new OptionRequest("기본", 10))
        );
        Product savedProduct = productService.create(request);

        // when
        assertDoesNotThrow(() ->
                productService.update(savedProduct.getId(), "매운 새우깡", 1300, "http://img2.jpg"));

        // then
        ProductOld updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertEquals("매운 새우깡", updatedProduct.getName());
        assertEquals(1300, updatedProduct.getPrice());
        assertEquals("http://img2.jpg", updatedProduct.getImageUrl());
    }

    @Test
    void 존재하지_않는_ID로_삭제해도_예외가_발생하지_않는다() {
        // given
        Long invalidId = 999L;

        // when & then
        assertDoesNotThrow(() -> productService.delete(invalidId));
    }

}
