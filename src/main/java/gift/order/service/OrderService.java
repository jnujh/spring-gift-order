package gift.order.service;

import gift.member.domain.Member;
import gift.option.domain.Option;
import gift.option.repository.OptionJpaRepository;
import gift.order.domain.Order;
import gift.order.dto.OrderRequest;
import gift.order.event.OrderPlacedEvent;
import gift.order.exception.OptionNotFoundException;
import gift.order.repository.OrderJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderJpaRepository orderJpaRepository;
    private final OptionJpaRepository optionJpaRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderJpaRepository orderJpaRepository, OptionJpaRepository optionJpaRepository, ApplicationEventPublisher eventPublisher) {
        this.orderJpaRepository = orderJpaRepository;
        this.optionJpaRepository = optionJpaRepository;
        this.eventPublisher = eventPublisher;
    }

    // 주문 생성
    @Transactional
    public Order placeOrder(Member member, OrderRequest request) {
        // 주문 옵션 조회
        Option option = optionJpaRepository.findById(request.optionId())
                .orElseThrow(() -> new OptionNotFoundException("존재하지 않는 옵션입니다. ID: " + request.optionId()));

        // 재고 차감
        option.subtract(request.quantity());

        // 주문 생성 및 저장
        Order order = Order.create(member, option, request.quantity(), request.message());
        orderJpaRepository.save(order);

        // 주문 완료 이벤트 발행
        eventPublisher.publishEvent(new OrderPlacedEvent(order.getId()));
        log.info("OrderPlacedEvent 발행. orderId: {}", order.getId());

        return order;
    }
}
