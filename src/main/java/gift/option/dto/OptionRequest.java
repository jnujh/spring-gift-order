package gift.option.dto;

import jakarta.validation.constraints.*;

public record OptionRequest(

        @NotBlank(message = "옵션 이름은 필수입니다.")
        @Size(max = 50, message = "옵션 이름은 최대 50자까지 입력 가능합니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9가-힣 ()\\[\\]\\+\\-\\&/_]+$",
                message = "허용되지 않은 특수 문자가 포함되어 있습니다."
        )
        String name,

        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        @Max(value = 99999999, message = "수량은 1억 미만이어야 합니다.")
        int quantity

) {}
