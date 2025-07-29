package gift.kakaomessage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import gift.order.domain.Order;

public record KakaoMessageRequest(
        @JsonProperty("template_object")
        DefaultTemplate templateObject
) {

    // 기본 템플릿 구조
    public record DefaultTemplate(
            @JsonProperty("object_type") String objectType,
            String text,
            Link link,
            @JsonProperty("button_title") String buttonTitle
    ) {
    }

    // 링크 구조
    public record Link(
            @JsonProperty("web_url") String webUrl,
            @JsonProperty("mobile_web_url") String mobileWebUrl
    ) {
    }

    // Order 객체로부터 KakaoMessageRequest 를 생성하는 정적 팩토리 메소드
    public static KakaoMessageRequest from(Order order) {
        String text = String.format("%s 상품의 주문이 완료되었습니다. (수량: %d개)",
                order.getOption().getProduct().getName(), order.getQuantity());

        Link link = new Link("http://localhost:8080", "http://localhost:8080");
        DefaultTemplate template = new DefaultTemplate("text", text, link, "주문 상세 보기");

        return new KakaoMessageRequest(template);
    }
}