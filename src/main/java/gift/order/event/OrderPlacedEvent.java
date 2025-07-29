package gift.order.event;

/**
 * 주문 성공 시 발행되는 이벤트
 * @param orderId 완료된 주문의 ID
 */
public record OrderPlacedEvent(Long orderId) {
}