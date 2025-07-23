package gift.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WishRequest(
        @NotNull(message = "productId는 필수입니다.")
        @Min(value = 1, message = "productId는 1 이상이어야 합니다.")
        Long productId
) {}
