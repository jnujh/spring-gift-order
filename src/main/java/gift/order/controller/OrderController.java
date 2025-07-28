package gift.order.controller;

import gift.global.annotation.LoginMember;
import gift.member.domain.Member;
import gift.order.domain.Order;
import gift.order.dto.OrderRequest;
import gift.order.dto.OrderResponse;
import gift.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @LoginMember Member member,
            @RequestBody OrderRequest request
            )
    {
        Order order = orderService.placeOrder(member, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderResponse.from(order));
    }

}
