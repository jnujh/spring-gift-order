package gift.order.service;

import gift.member.domain.Member;
import gift.option.domain.Option;
import gift.option.repository.OptionJpaRepository;
import gift.order.domain.Order;
import gift.order.dto.OrderRequest;
import gift.order.repository.OrderJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderJpaRepository orderJpaRepository;
    private final OptionJpaRepository optionJpaRepository;

    public OrderService(OrderJpaRepository orderJpaRepository, OptionJpaRepository optionJpaRepository) {
        this.orderJpaRepository = orderJpaRepository;
        this.optionJpaRepository = optionJpaRepository;
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
        return orderJpaRepository.save(order);
    }
}
