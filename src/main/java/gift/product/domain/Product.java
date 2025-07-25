package gift.product.domain;

import gift.option.domain.Option;
import gift.wish.domain.Wish;
import gift.option.dto.OptionRequest;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
public class Product {

    private static final int MIN_PRICE = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 15)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Wish> wishes = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Option> options = new ArrayList<>();

    protected Product() {
    }

    // 새로운 상품 생성 (정적 팩토리 메서드)
    public static Product create(String name, int price, String imageUrl, List<OptionRequest> optionRequests) {
        validate(name, price, imageUrl);

        if (optionRequests == null || optionRequests.isEmpty()) {
            throw new IllegalArgumentException("상품은 최소 하나의 옵션을 포함해야 합니다.");
        }

        Product product = new Product();
        product.name = name;
        product.price = price;
        product.imageUrl = imageUrl;

        for (OptionRequest optionRequest : optionRequests) {
            Option option = Option.create(product, optionRequest.name(), optionRequest.quantity());
            product.options.add(option);
        }

        return product;
    }

    // 상품 수정
    public void update(String name, int price, String imageUrl) {
        validate(name, price, imageUrl);
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    // 상품 유효성 검사
    private static void validate(String name, int price, String imageUrl) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("이름은 필수입니다.");
        if (price <= MIN_PRICE) throw new IllegalArgumentException("가격은 1원 이상이어야 합니다.");
        if (imageUrl == null || imageUrl.isBlank()) throw new IllegalArgumentException("이미지 URL은 필수입니다.");
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public List<Wish> getWishes() { return wishes; }
    public List<Option> getOptions() { return options; }
}
