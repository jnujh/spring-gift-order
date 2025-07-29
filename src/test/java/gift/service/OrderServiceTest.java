package gift.service;

import gift.member.domain.Member;
import gift.option.domain.Option;
import gift.option.dto.OptionRequest;
import gift.option.repository.OptionJpaRepository;
import gift.order.domain.Order;
import gift.order.dto.OrderRequest;
import gift.order.event.OrderPlacedEvent;
import gift.order.exception.InsufficientStockException;
import gift.order.exception.OptionNotFoundException;
import gift.order.repository.OrderJpaRepository;
import gift.order.service.OrderService;
import gift.product.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderJpaRepository orderJpaRepository;

    @Mock
    private OptionJpaRepository optionJpaRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Member testMember;
    private Option testOption;

    @BeforeEach
    void setUp() {
        testMember = Member.create("test@example.com", "password123!");
        Product testProduct = Product.create(
                "Test Product", 1000, "image.jpg",
                List.of(new OptionRequest("Test Option", 10))
        );
        testOption = testProduct.getOptions().get(0);
    }

    @Test
    @DisplayName("주문이 성공하면 재고가 차감되고 이벤트가 발행된다")
    void placeOrder_Succeeds() {
        OrderRequest request = new OrderRequest(1L, 5, "감사합니다");

        when(optionJpaRepository.findById(1L)).thenReturn(Optional.of(testOption));
        when(orderJpaRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.placeOrder(testMember, request);

        assertThat(result.getQuantity()).isEqualTo(5);
        assertThat(result.getMember()).isEqualTo(testMember);

        assertThat(testOption.getQuantity()).isEqualTo(5);

        verify(orderJpaRepository, times(1)).save(any(Order.class));
        verify(eventPublisher, times(1)).publishEvent(any(OrderPlacedEvent.class));
    }

    @Test
    @DisplayName("주문하려는 옵션이 존재하지 않으면 예외가 발생한다")
    void placeOrder_Fails_When_OptionNotFound() {
        OrderRequest request = new OrderRequest(999L, 5, "없는 옵션 주문");
        when(optionJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(testMember, request))
                .isInstanceOf(OptionNotFoundException.class)
                .hasMessageContaining("존재하지 않는 옵션입니다.");

        verify(orderJpaRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("재고가 부족하면 예외가 발생한다")
    void placeOrder_Fails_When_InsufficientStock() {
        OrderRequest request = new OrderRequest(1L, 11, "재고보다 많이 주문");
        when(optionJpaRepository.findById(1L)).thenReturn(Optional.of(testOption));

        assertThatThrownBy(() -> orderService.placeOrder(testMember, request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("재고가 부족합니다.");

        assertThat(testOption.getQuantity()).isEqualTo(10);

        verify(orderJpaRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}