package gift.order.service;

import gift.kakaomessage.exception.KakaoMessageException;
import gift.kakaomessage.service.KakaoMessageService;
import gift.order.domain.Order;
import gift.order.event.OrderPlacedEvent;
import gift.order.repository.OrderJpaRepository;
import gift.wish.repository.WishJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final OrderJpaRepository orderRepository;
    private final WishJpaRepository wishRepository;
    private final KakaoMessageService kakaoMessageService;

    public OrderEventListener(OrderJpaRepository orderRepository, WishJpaRepository wishRepository, KakaoMessageService kakaoMessageService) {
        this.orderRepository = orderRepository;
        this.wishRepository = wishRepository;
        this.kakaoMessageService = kakaoMessageService;
    }

    /**
     * 주문 완료 이벤트 수신하면 위시리스트 처리
     */
    @TransactionalEventListener
    public void handleWishlist(OrderPlacedEvent event) {
        log.info("주문 완료 이벤트 수신 (위시리스트 처리): orderId={}", event.orderId());
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문에 대한 이벤트가 수신되었습니다. 주문 ID: " + event.orderId()));

        wishRepository.deleteByMemberAndProduct(order.getMember(), order.getOption().getProduct());
    }

    /**
     * 주문 완료 이벤트 수신하면 카카오 메시지 발송
     */
    @TransactionalEventListener
    public void handleKakaoMessage(OrderPlacedEvent event) {
        log.info("주문 완료 이벤트 수신 (카카오 메시지 발송): orderId={}", event.orderId());
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문에 대한 이벤트가 수신되었습니다. 주문 ID: " + event.orderId()));

        try {
            kakaoMessageService.sendOrderCompletionMessage(order.getMember().getKakaoAccessToken(), order);
        } catch (KakaoMessageException e) {
            log.error("주문 완료 카카오톡 메시지 발송 실패. orderId: {}, member: {}",
                    order.getId(), order.getMember().getEmail(), e);
        }
    }
}