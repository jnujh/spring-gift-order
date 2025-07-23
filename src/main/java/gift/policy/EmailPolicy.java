package gift.policy;

public class EmailPolicy {

    // 이메일 형식을 검사하는 정규표현식
    public static final String EMAIL_REGEX =
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    // 에러 메시지
    public static final String EMAIL_RULE_MESSAGE =
            "올바른 이메일 형식이 아닙니다.";
}
