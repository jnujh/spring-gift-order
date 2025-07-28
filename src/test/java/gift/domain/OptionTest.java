package gift.domain;

import gift.option.dto.OptionRequest;
import gift.option.domain.Option;
import gift.order.exception.InsufficientStockException;
import gift.product.domain.Product;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class OptionTest {

    @Test
    void 정상적으로_수량이_차감된다() {
        // given
        Product dummyProduct = Product.create("상품", 1000, "img.jpg", List.of(new OptionRequest("기본", 10)));
        Option option = Option.create(dummyProduct, "색상[블랙]", 10);

        // when
        option.subtract(3);

        // then
        assertThat(option.getQuantity()).isEqualTo(7);
    }

    @Test
    void 차감수량이_0이하면_예외가_발생한다() {
        // given
        Product dummyProduct = Product.create("상품", 1000, "img.jpg", List.of(new OptionRequest("기본", 10)));
        Option option = Option.create(dummyProduct, "색상[화이트]", 10);

        // when & then
        assertThatThrownBy(() -> option.subtract(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("차감 수량은 0보다 커야 합니다.");
    }

    @Test
    void 차감수량이_재고보다_많으면_예외가_발생한다() {
        // given
        Product dummyProduct = Product.create("상품", 1000, "img.jpg", List.of(new OptionRequest("기본", 10)));
        Option option = Option.create(dummyProduct, "사이즈[XL]", 5);

        // when & then
        assertThatThrownBy(() -> option.subtract(10))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("재고가 부족합니다");
    }
}
