package gift.member.service;

import gift.member.domain.Member;
import gift.global.exception.ForbiddenException;
import gift.member.repository.MemberJpaRepository;
import gift.auth.JwtTokenProvider;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class MemberService {

    private final MemberJpaRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MessageSource messageSource;

    public MemberService(MemberJpaRepository memberRepository,
                         PasswordEncoder passwordEncoder,
                         JwtTokenProvider jwtTokenProvider,
                         MessageSource messageSource) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.messageSource = messageSource;
    }

    // 회원가입
    public String register(String email, String rawPassword) {

        String encodedPassword = passwordEncoder.encode(rawPassword);
        Member member = Member.withEncodedPassword(email, encodedPassword);

        Member saved = memberRepository.save(member);
        return jwtTokenProvider.createToken(saved.getId());
    }

    // 로그인
    public String login(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException(getMessage("member.login.failed")));

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new ForbiddenException(getMessage("member.login.failed"));
        }

        return jwtTokenProvider.createToken(member.getId());
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, Locale.getDefault());
    }
}
