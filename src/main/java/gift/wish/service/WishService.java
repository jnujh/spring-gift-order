package gift.wish.service;

import gift.wish.exception.AlreadyWishedException;
import gift.wish.exception.UnauthorizedWishAccessException;
import gift.member.domain.Member;
import gift.product.domain.Product;
import gift.member.repository.MemberJpaRepository;
import gift.product.repository.ProductJpaRepository;
import gift.wish.domain.Wish;
import gift.wish.repository.WishJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WishService {

    private final WishJpaRepository wishRepository;
    private final ProductJpaRepository productRepository;
    private final MemberJpaRepository memberRepository;

    public WishService(WishJpaRepository wishRepository,
                       ProductJpaRepository productRepository,
                       MemberJpaRepository memberRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * 위시리스트에 상품 추가
     * 이미 찜한 경우 예외 발생
     */
    public Wish addWish(Long memberId, Long productId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        if (wishRepository.existsByMemberAndProduct(member, product)) {
            throw new AlreadyWishedException();
        }

        return wishRepository.save(Wish.create(member, product));
    }

    /**
     * 위시리스트에서 상품 제거
     * 멱등성 보장: 존재하지 않아도 예외 없이 삭제 시도
     * 다른 사용자의 찜 항목은 삭제할 수 없음
     */
    public void removeWish(Long wishId, Long memberId) {
        Wish wish = wishRepository.findById(wishId).orElse(null);

        if (wish == null) {
            // 존재하지 않으면 무시 (멱등성 보장)
            return;
        }

        if (!wish.getMember().getId().equals(memberId)) {
            throw new UnauthorizedWishAccessException("다른 사용자의 위시리스트 항목은 삭제할 수 없습니다.");
        }
    }

    /**
     * 사용자별 위시리스트 조회
     */
    public Page<Wish> getWishlist(Long memberId, Pageable pageable) {

        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException("회원을 찾을 수 없습니다.");
        }

        return wishRepository.findByMemberId(memberId, pageable);
    }
}
