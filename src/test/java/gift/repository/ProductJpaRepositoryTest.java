package gift.repository;

import gift.domain.Product;
import gift.dto.OptionRequest;
import gift.dto.ProductRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
class ProductJpaRepositoryTest {

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Test
    @DisplayName("상품을 저장할 수 있다")
    void save() {
        // given
        Product product = Product.create("상품A", 1000, "image.jpg", List.of(new OptionRequest("기본", 10)));

        // when
        Product saved = productJpaRepository.save(product);

        // then
        assertAll(
                () -> assertThat(saved.getId()).isNotNull(),
                () -> assertThat(saved.getName()).isEqualTo("상품A"),
                () -> assertThat(saved.getPrice()).isEqualTo(1000),
                () -> assertThat(saved.getImageUrl()).isEqualTo("image.jpg")
        );
    }

    @Test
    @DisplayName("상품을 ID로 조회할 수 있다")
    void findById() {
        // given
        Product product = Product.create("상품A", 1000, "image.jpg",List.of(new OptionRequest("기본", 10)));
        Product saved = productJpaRepository.save(product);

        // when
        Optional<Product> found = productJpaRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("상품A");
    }

    @Test
    @DisplayName("저장된 모든 상품을 조회할 수 있다")
    void findAll() {
        // given
        productJpaRepository.save(Product.create("상품A", 1000, "a.jpg",List.of(new OptionRequest("기본", 10))));
        productJpaRepository.save(Product.create("상품B", 2000, "b.jpg",List.of(new OptionRequest("기본", 10))));

        // when
        List<Product> products = productJpaRepository.findAll();

        // then
        assertThat(products).hasSize(2);
    }

    @Test
    @DisplayName("상품 ID 존재 여부를 확인할 수 있다")
    void existsById() {
        // given
        Product product = productJpaRepository.save(Product.create("상품", 1000, "image.jpg",List.of(new OptionRequest("기본", 10))));

        // when & then
        assertThat(productJpaRepository.existsById(product.getId())).isTrue();
        assertThat(productJpaRepository.existsById(999L)).isFalse();
    }

    @Test
    @DisplayName("상품을 삭제할 수 있다")
    void deleteById() {
        // given
        Product saved = productJpaRepository.save(Product.create("상품", 1000, "image.jpg",List.of(new OptionRequest("기본", 10))));
        Long id = saved.getId();

        // when
        productJpaRepository.deleteById(id);

        // then
        assertThat(productJpaRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("상품 정보를 수정할 수 있다 (변경 감지)")
    void update() {
        // given
        Product product = productJpaRepository.save(Product.create("상품", 1000, "image.jpg",List.of(new OptionRequest("기본", 10))));

        // when
        product.update("수정된상품", 2000, "new.jpg");
        // 변경 감지는 트랜잭션 커밋 시 일어남 (@DataJpaTest는 기본적으로 rollback)
        Product updatedProduct = productJpaRepository.findById(product.getId()).orElseThrow();

        // then
        assertThat(updatedProduct.getName()).isEqualTo("수정된상품");
        assertThat(updatedProduct.getPrice()).isEqualTo(2000);
        assertThat(updatedProduct.getImageUrl()).isEqualTo("new.jpg");
    }
}
