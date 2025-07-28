package gift.option.domain;

import gift.order.exception.InsufficientStockException;
import gift.product.domain.Product;
import jakarta.persistence.*;

@Entity
@Table(name = "options")
public class Option {

    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_QUANTITY = 100_000_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(nullable = false)
    private int quantity;

    protected Option() {

    }

    // 정적 팩토리 메소드
    public static Option create(Product product, String name, int quantity) {
        validateName(name);
        validateQuantity(quantity);

        Option option = new Option();
        option.product = product;
        option.name = name;
        option.quantity = quantity;

        return option;
    }

    // subtract 메소드
    public void subtract(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("차감 수량은 0보다 커야 합니다.");
        }

        if (this.quantity < amount) {
            throw new InsufficientStockException("재고가 부족합니다. 현재 재고: " + this.quantity);
        }

        this.quantity -= amount;
    }

    // 이름 유효성 검사
    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("옵션 이름은 1~50자여야 합니다.");
        }

        if (!name.matches("^[a-zA-Z0-9가-힣 ()\\[\\]\\+\\-\\&/_]+$")) {
            throw new IllegalArgumentException("허용되지 않은 문자가 포함된 옵션명입니다.");
        }
    }

    // 수량 유효성 검사
    private static void validateQuantity(int quantity) {
        if (quantity < 1 || quantity >= MAX_QUANTITY) {
            throw new IllegalArgumentException("수량은 1 이상 1억 미만이어야 합니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }
}
