package gift.member.domain;

import gift.member.policy.EmailPolicy;
import gift.member.policy.PasswordPolicy;

public class MemberOld {

    private final Long id;
    private final String email;
    private final String password;

    private MemberOld(Long id, String email, String password) {
        this.id = id;
        this.email = email;
        this.password = password;
    }

    // DB 에서 조회된 데이터를 복원할 때 사용 (검증 생략)
    public static MemberOld withId(Long id, String email, String password) {
        return new MemberOld(id, email, password);
    }

    // 암호화된 비밀번호를 이용해 ID 없이 Member 생성 (유효성 검사는 외부에서 수행)
    public static MemberOld withEncodedPassword(String email, String encodedPassword) {
        return new MemberOld(null, email, encodedPassword);
    }

    // 유효성 검사만 수행 (암호화 전 사용)
    public static void validateForRegister(String email, String rawPassword) {
        validateEmail(email);
        validatePassword(rawPassword);
    }

    // 유효성 검사 내부 로직
    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        if (!email.matches(EmailPolicy.EMAIL_REGEX)) {
            throw new IllegalArgumentException(EmailPolicy.EMAIL_RULE_MESSAGE);
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }
        if (!password.matches(PasswordPolicy.PASSWORD_REGEX)) {
            throw new IllegalArgumentException(PasswordPolicy.PASSWORD_RULE_MESSAGE);
        }
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
