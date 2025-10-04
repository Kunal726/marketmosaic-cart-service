package com.projects.marketmosaic.dto.req;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data

public class UpdateCartItemRequest {

    @NotNull(message = "Cart item ID is required")
    private Long cartItemId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

}

