package gift.order.service;

import gift.kakaomessage.service.KakaoMessageService;
import gift.member.domain.Member;
import gift.option.domain.Option;
import gift.option.repository.OptionJpaRepository;
import gift.order.domain.Order;
import gift.order.dto.OrderRequest;
import gift.order.repository.OrderJpaRepository;
import gift.wish.repository.WishJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderJpaRepository orderJpaRepository;
    private final OptionJpaRepository optionJpaRepository;
    private final WishJpaRepository wishJpaRepository;
    private final KakaoMessageService kakaoMessageService;

    public OrderService(OrderJpaRepository orderJpaRepository, OptionJpaRepository optionJpaRepository, WishJpaRepository wishJpaRepository, KakaoMessageService kakaoMessageService) {
        this.orderJpaRepository = orderJpaRepository;
        this.optionJpaRepository = optionJpaRepository;
        this.wishJpaRepository = wishJpaRepository;
        this.kakaoMessageService = kakaoMessageService;
    }

    // 주문 생성
    public Order placeOrder(Member member, OrderRequest request) {

        // 주문 옵션 조회
        Option option = optionJpaRepository.findById(request.optionId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옵션입니다. ID: " + request.optionId()));

        // 수량 차감
        option.subtract(request.quantity());

        // Order (주문 생성)
        Order order = Order.create(member, option, request.quantity(), request.message());
        orderJpaRepository.save(order);

        // 위시리스트에 존재하던 상품이면 위시리스트에서 삭제
        wishJpaRepository.deleteByMemberAndProduct(member, option.getProduct());

        // 주문 완료 카카오 메시지 전송
        kakaoMessageService.sendOrderCompletionMessage(member.getKakaoAccessToken(), order);

        return order;
    }
}
