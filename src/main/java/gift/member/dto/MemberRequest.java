package gift.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static gift.policy.EmailPolicy.*;
import static gift.policy.PasswordPolicy.*;

public record MemberRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Pattern(regexp = EMAIL_REGEX, message = EMAIL_RULE_MESSAGE)
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_RULE_MESSAGE)
        String password

) {}
