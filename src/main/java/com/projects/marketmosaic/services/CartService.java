package com.projects.marketmosaic.services;

import com.projects.marketmosaic.common.dto.cart.CartDTO;
import com.projects.marketmosaic.common.dto.resp.BaseRespDTO;
import com.projects.marketmosaic.dto.CheckoutCartRespDto;
import com.projects.marketmosaic.dto.req.AddToCartRequest;
import com.projects.marketmosaic.dto.req.DeleteCartItemRequest;
import com.projects.marketmosaic.dto.req.UpdateCartItemRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

public interface CartService {
    CartDTO getCart(HttpServletRequest request);

    BaseRespDTO addToCart(HttpServletRequest request, HttpServletResponse response, @Valid AddToCartRequest addToCartRequest);

    BaseRespDTO updateCartItem(HttpServletRequest request, @Valid UpdateCartItemRequest updateCartItemRequest);

    BaseRespDTO deleteCartItem(HttpServletRequest request, @Valid DeleteCartItemRequest deleteCartItemRequest);

    CheckoutCartRespDto checkoutCart(HttpServletRequest request, HttpServletResponse response);
}
