package com.projects.marketmosaic.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeleteCartItemRequest {

    @NotNull(message = "Cart item ID is required")
    private Long cartItemId;
}
