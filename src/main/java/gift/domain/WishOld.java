package gift.domain;

import java.time.LocalDateTime;

public class WishOld {

    private final Long id;                    // 찜 항목 고유 ID (wishId)
    private final Long memberId;              // 사용자 ID (FK to member)
    private final Long productId;             // 상품 ID (FK to product)
    private final LocalDateTime createdAt;  // 찜한 시간 (정렬 기준)

    private WishOld(Long id, Long memberId, Long productId, LocalDateTime createdAt) {
        this.id = id;
        this.memberId = memberId;
        this.productId = productId;
        this.createdAt = createdAt;
    }

    public static WishOld of(Long id, Long memberId, Long productId, LocalDateTime createdAt) {
        return new WishOld(id, memberId, productId, createdAt);
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public Long getProductId() { return productId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
