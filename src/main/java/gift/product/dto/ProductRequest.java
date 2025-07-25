package gift.product.dto;

import gift.option.dto.OptionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record ProductRequest(

        @NotBlank(message = "상품 이름은 필수입니다.")
        @Size(max = 15, message = "상품 이름은 최대 15자까지 입력 가능합니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9가-힣 ()\\[\\]\\+\\-\\&/_]*$",
                message = "상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다."
        )
        String name,

        @Min(value = 1, message = "상품 가격은 1원 이상이어야 합니다.")
        int price,

        @NotBlank(message = "이미지 URL은 필수입니다.")
        @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
        String imageUrl,

        @NotEmpty(message = "옵션은 최소 1개 이상 입력해야 합니다.")
        List<@Valid OptionRequest> options

) {}
