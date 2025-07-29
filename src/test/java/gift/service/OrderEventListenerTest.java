package gift.service;

import gift.kakaomessage.exception.KakaoMessageException;
import gift.kakaomessage.service.KakaoMessageService;
import gift.member.domain.Member;
import gift.option.domain.Option;
import gift.option.dto.OptionRequest;
import gift.order.domain.Order;
import gift.order.event.OrderPlacedEvent;
import gift.order.repository.OrderJpaRepository;
import gift.order.service.OrderEventListener;
import gift.product.domain.Product;
import gift.wish.repository.WishJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @InjectMocks
    private OrderEventListener orderEventListener;

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private WishJpaRepository wishRepository;

    @Mock
    private KakaoMessageService kakaoMessageService;

    private Member testMember;
    private Product testProduct;
    private Order testOrder;
    private OrderPlacedEvent testEvent;

    @BeforeEach
    void setUp() {
        testMember = Member.create("test@example.com", "password123!");
        testMember.updateKakaoAccessToken("fake-kakao-access-token");

        testProduct = Product.create(
                "Test Product", 1000, "image.jpg",
                List.of(new OptionRequest("Test Option", 10))
        );
        Option testOption = testProduct.getOptions().get(0);
        testOrder = Order.create(testMember, testOption, 1, "Test Message");

        testEvent = new OrderPlacedEvent(1L);
    }

    @Test
    @DisplayName("주문 완료 이벤트 수신 시 위시리스트에서 해당 상품을 삭제한다")
    void handleWishlist_deletesWish() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        orderEventListener.handleWishlist(testEvent);

        verify(wishRepository, times(1)).deleteByMemberAndProduct(testMember, testProduct);
    }

    @Test
    @DisplayName("주문 완료 이벤트 수신 시 카카오 메시지를 전송한다")
    void handleKakaoMessage_sendsMessage() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        orderEventListener.handleKakaoMessage(testEvent);

        verify(kakaoMessageService, times(1))
                .sendOrderCompletionMessage("fake-kakao-access-token", testOrder);
    }

    @Test
    @DisplayName("카카오 메시지 전송 실패 시 예외를 던지지 않고 처리한다")
    void handleKakaoMessage_doesNotThrow_onFailure() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        doThrow(new KakaoMessageException("API call failed"))
                .when(kakaoMessageService).sendOrderCompletionMessage(anyString(), any(Order.class));

        assertDoesNotThrow(() -> orderEventListener.handleKakaoMessage(testEvent));

        verify(kakaoMessageService, times(1)).sendOrderCompletionMessage(anyString(), any(Order.class));
    }

    @Test
    @DisplayName("이벤트의 주문 ID가 유효하지 않으면 예외가 발생한다")
    void handleEvent_Fails_When_OrderNotFound() {
        OrderPlacedEvent invalidEvent = new OrderPlacedEvent(999L);
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderEventListener.handleWishlist(invalidEvent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("존재하지 않는 주문에 대한 이벤트가 수신되었습니다.");

        assertThatThrownBy(() -> orderEventListener.handleKakaoMessage(invalidEvent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("존재하지 않는 주문에 대한 이벤트가 수신되었습니다.");
    }
}