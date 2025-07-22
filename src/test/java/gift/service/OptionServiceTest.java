package gift.service;

import gift.domain.Option;
import gift.domain.Product;
import gift.dto.OptionRequest;
import gift.dto.ProductRequest;
import gift.repository.OptionJpaRepository;
import gift.repository.ProductJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(OptionService.class)
@Transactional
class OptionServiceTest {

    @Autowired
    private OptionService optionService;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private OptionJpaRepository optionRepository;

    @Autowired
    private EntityManager em;

    private Product savedProduct;

    @BeforeEach
    void setUp() {
        ProductRequest request = new ProductRequest(
                "차렵 이불",
                10000,
                "img.jpg",
                List.of(new OptionRequest("기본", 100))
        );

        Product product = Product.create(
                request.name(),
                request.price(),
                request.imageUrl(),
                request.options()
        );
        savedProduct = productRepository.save(product);
    }

    @Test
    void 옵션을_정상적으로_생성할_수_있다() {
        // when
        Long id = optionService.createOption(savedProduct.getId(), "색상[블루]", 100);

        // then
        Option option = optionRepository.findById(id).orElseThrow();
        assertThat(option.getName()).isEqualTo("색상[블루]");
        assertThat(option.getQuantity()).isEqualTo(100);
        assertThat(option.getProduct().getId()).isEqualTo(savedProduct.getId());
    }

    @Test
    void 옵션이_중복되면_예외를_던진다() {
        // given
        optionService.createOption(savedProduct.getId(), "중복", 1);

        // expect
        assertThatThrownBy(() ->
                optionService.createOption(savedProduct.getId(), "중복", 5)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 상품ID로_옵션_목록을_조회할_수_있다() {
        // given
        optionService.createOption(savedProduct.getId(), "색상[민트]", 10);
        optionService.createOption(savedProduct.getId(), "색상[옐로우]", 20);

        // when
        List<?> list = optionService.getOptionsByProductId(savedProduct.getId());

        // then
        assertThat(list).hasSize(2);
    }

    @Test
    void 옵션ID로_삭제할_수_있다() {
        // given
        Long optionId = optionService.createOption(savedProduct.getId(), "삭제옵션", 10);

        // when
        optionService.deleteOption(optionId);

        // then
        assertThat(optionRepository.findById(optionId)).isEmpty();
    }

    @Test
    void 존재하지_않는_옵션을_삭제하려하면_예외발생() {
        assertThatThrownBy(() ->
                optionService.deleteOption(9999L)
        ).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 옵션이_한개뿐이면_삭제할_수_없다() {
        // given
        Long optionId = savedProduct.getOptions().get(0).getId();

        // when & then
        assertThatThrownBy(() -> optionService.deleteOption(optionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("상품은 최소 1개의 옵션을 유지해야 하므로 삭제할 수 없습니다.");
    }
}
