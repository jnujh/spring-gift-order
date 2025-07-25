package gift.repository;

import gift.domain.WishOld;

import java.util.List;
import java.util.Optional;

public interface WishRepository {

    WishOld addWish(Long memberId, Long productId);

    boolean existsInWishlist(Long memberId, Long productId);

    List<WishOld> getWishlistByMemberId(Long memberId);

    Optional<WishOld> findById(Long id);

    void removeByMemberIdAndWishId(Long memberId, Long wishId);

}
