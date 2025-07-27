package gift.wish.dto;

import gift.wish.domain.Wish;

public record WishResponse(
        Long wishId,
        Long productId
) {
    public static WishResponse from(Wish wish) {
        return new WishResponse(
                wish.getId(),
                wish.getProduct().getId()
        );
    }
}
