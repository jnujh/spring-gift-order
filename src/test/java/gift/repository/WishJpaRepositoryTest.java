package gift.repository;

import gift.member.domain.Member;
import gift.member.repository.MemberJpaRepository;
import gift.product.domain.Product;
import gift.wish.domain.Wish;
import gift.option.dto.OptionRequest;
import gift.product.repository.ProductJpaRepository;
import gift.wish.repository.WishJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class WishJpaRepositoryTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private WishJpaRepository wishJpaRepository;

    @Test
    @DisplayName("Wish를 저장할 수 있다")
    void save() {
        // given
        Member member = memberJpaRepository.save(Member.create("user@example.com", "Password123!"));
        Product product = productJpaRepository.save(Product.create("선물", 1000, "gift.jpg", List.of(new OptionRequest("기본", 10))));

        // when
        Wish wish = wishJpaRepository.save(Wish.create(member, product));

        // then
        assertThat(wish.getId()).isNotNull();
        assertThat(wish.getMember().getId()).isEqualTo(member.getId());
        assertThat(wish.getProduct().getId()).isEqualTo(product.getId());
    }

    @Test
    @DisplayName("중복 찜 여부를 확인할 수 있다")
    void existsByMemberAndProduct() {
        // given
        Member member = memberJpaRepository.save(Member.create("user@example.com", "Password123!"));
        Product product = productJpaRepository.save(Product.create("선물", 1000, "gift.jpg",List.of(new OptionRequest("기본", 10))));
        wishJpaRepository.save(Wish.create(member, product));

        // when
        boolean exists = wishJpaRepository.existsByMemberAndProduct(member, product);

        // then
        assertThat(exists).isTrue();
    }

//    @Test
//    @DisplayName("사용자의 모든 찜 항목을 ID 기준 내림차순으로 조회할 수 있다")
//    void findAllByMemberOrderByIdDesc() {
//        // given
//        Member member = memberJpaRepository.save(Member.create("user@example.com", "Password123!"));
//        Product product1 = productJpaRepository.save(Product.create("A", 1000, "a.jpg"));
//        Product product2 = productJpaRepository.save(Product.create("B", 2000, "b.jpg"));
//
//        wishJpaRepository.save(Wish.create(member, product1));
//        wishJpaRepository.save(Wish.create(member, product2));
//
//        // when
//        List<Wish> wishes = wishJpaRepository.findAllByMemberIdAndOrderByIdDesc(member);
//
//        // then
//        assertThat(wishes).hasSize(2);
//        assertThat(wishes.get(0).getProduct().getName()).isEqualTo("B");
//        assertThat(wishes.get(1).getProduct().getName()).isEqualTo("A");
//    }

    @Test
    @DisplayName("사용자의 찜 항목을 안전하게 삭제할 수 있다")
    void deleteByIdAndMemberId() {
        // given
        Member member = memberJpaRepository.save(Member.create("user@example.com", "Password123!"));
        Product product = productJpaRepository.save(Product.create("선물", 1000, "gift.jpg",List.of(new OptionRequest("기본", 10))));
        Wish wish = wishJpaRepository.save(Wish.create(member, product));

        // when
        wishJpaRepository.deleteByIdAndMemberId(wish.getId(), member.getId());

        // then
        boolean exists = wishJpaRepository.existsById(wish.getId());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("사용자의 위시리스트를 페이지 단위로 조회할 수 있다.")
    void findByMemberId() {
        // given
        Member member = memberJpaRepository.save(Member.create("user@example.com", "Password123!"));

        for (int i = 1; i <= 5; i++) {
            Product product = productJpaRepository.save(Product.create("상품" + i, i * 1000, "img" + i + ".jpg",List.of(new OptionRequest("기본", 10))));
            wishJpaRepository.save(Wish.create(member, product));
        }

        // when
        PageRequest pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "id"));
        Page<Wish> result = wishJpaRepository.findByMemberId(member.getId(), pageable);

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent().get(0).getProduct().getName()).isEqualTo("상품5");

    }
}
