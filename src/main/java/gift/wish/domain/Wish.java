package gift.wish.domain;

import gift.member.domain.Member;
import gift.product.domain.Product;
import jakarta.persistence.*;

@Entity
@Table(name = "wish")
public class Wish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연관관계: Wish → Member (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 연관관계: Wish → Product (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    protected Wish() {
    }

    public static Wish create(Member member, Product product) {
        Wish wish = new Wish();
        wish.member = member;
        wish.product = product;

        member.getWishes().add(wish);
        product.getWishes().add(wish);

        return wish;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public Product getProduct() {
        return product;
    }
}
