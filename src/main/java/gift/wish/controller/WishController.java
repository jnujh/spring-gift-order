package gift.wish.controller;

import gift.global.annotation.LoginMember;
import gift.member.domain.Member;
import gift.wish.domain.Wish;
import gift.wish.dto.WishRequest;
import gift.wish.dto.WishResponse;
import gift.wish.service.WishService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/wishes")
public class WishController {

    private final WishService wishService;

    public WishController(WishService wishService) {
        this.wishService = wishService;
    }

    /**
     * 사용자 위시리스트 조회 (페이지네이션 적용)
     */
    @GetMapping
    public ResponseEntity<Page<WishResponse>> getWishlist(@LoginMember Member member,
                                                          @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<Wish> wishPage = wishService.getWishlist(member.getId(), pageable);

        Page<WishResponse> responsePage = wishPage.map(WishResponse::from);

        return ResponseEntity.ok(responsePage);
    }

    /**
     * 찜 추가
     */
    @PostMapping
    public ResponseEntity<WishResponse> addWish(
            @RequestBody @Valid WishRequest request,
            @LoginMember Member member
    ) {
        Wish wish = wishService.addWish(member.getId(), request.productId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(WishResponse.from(wish));
    }

    /**
     * 찜 삭제
     */
    @DeleteMapping("/{wishId}")
    public ResponseEntity<Void> removeWish(
            @PathVariable Long wishId,
            @LoginMember Member member
    ) {
        wishService.removeWish(wishId, member.getId());
        return ResponseEntity.noContent().build();
    }
}
