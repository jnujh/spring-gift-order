package gift.resolver;

import gift.annotation.LoginMember;
import gift.domain.Member;
import gift.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final MemberService memberService;

    public LoginMemberArgumentResolver(MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMember.class)
                && parameter.getParameterType().equals(Member.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = ((ServletWebRequest) webRequest).getRequest();
        Object memberIdAttr = request.getAttribute("memberId");

        if (memberIdAttr == null) {
            throw new IllegalArgumentException("로그인 정보가 존재하지 않습니다.");
        }

        Long memberId;

        try {
            memberId = (Long)memberIdAttr;
        } catch (ClassCastException e) {
            throw new IllegalStateException("잘못된 형식의 memberId 입니다.");
        }

        return memberService.findById(memberId);
    }
}
