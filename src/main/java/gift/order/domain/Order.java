package gift.order.domain;

import gift.member.domain.Member;
import gift.option.domain.Option;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders") // order는 SQL 예약어
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private Option option;

    @Column(nullable = false)
    private int quantity;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime orderDateTime;

    protected Order() {
    }

    public static Order create(Member member, Option option, int quantity, String message) {

        validateQuantity(quantity);

        Order order = new Order();
        order.member = member;
        order.option = option;
        order.quantity = quantity;
        order.message = message;
        order.orderDateTime = LocalDateTime.now();

        // 양방향 관계를 위해 추가
        member.getOrders().add(order);

        return order;
    }

    // quantity 유효성 검사 (양수 강제)
    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("주문 수량은 1 이상이어야 합니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public Option getOption() {
        return option;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getOrderDateTime() {
        return orderDateTime;
    }
}
