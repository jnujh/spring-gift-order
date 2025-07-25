package gift.policy;

public class PasswordPolicy {

    public static final int MIN_LENGTH = 8;

    public static final String PASSWORD_REGEX =
            "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\-\\[\\]{};':\"\\\\|,.<>/?]).{" + MIN_LENGTH + ",}$";

    public static final String PASSWORD_RULE_MESSAGE =
            "올바르지 않은 비밀번호 형식입니다. (영문자, 숫자, 특수문자를 모두 포함, " + MIN_LENGTH + "자 이상)";
}
