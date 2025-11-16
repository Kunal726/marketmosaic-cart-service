package com.projects.marketmosaic.controller;

import com.projects.marketmosaic.common.dto.cart.CartDTO;
import com.projects.marketmosaic.common.dto.resp.BaseRespDTO;
import com.projects.marketmosaic.dto.CheckoutCartRespDto;
import com.projects.marketmosaic.dto.req.AddToCartRequest;
import com.projects.marketmosaic.dto.req.DeleteCartItemRequest;
import com.projects.marketmosaic.dto.req.UpdateCartItemRequest;
import com.projects.marketmosaic.services.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/user/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public ResponseEntity<CartDTO> getCart(HttpServletRequest request) {
        CartDTO cart = cartService.getCart(request);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    public ResponseEntity<BaseRespDTO> addToCart(@Valid @RequestBody AddToCartRequest request,
                                                 HttpServletRequest httpRequest, HttpServletResponse response) {
        BaseRespDTO resp = cartService.addToCart(httpRequest, response, request);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/items")
    public ResponseEntity<BaseRespDTO> updateCartItem(@Valid @RequestBody UpdateCartItemRequest request,
                                            HttpServletRequest httpRequest) {
        BaseRespDTO resp = cartService.updateCartItem(httpRequest, request);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/items")
    public ResponseEntity<BaseRespDTO> deleteCartItem(@Valid @RequestBody DeleteCartItemRequest request,
                                            HttpServletRequest httpRequest) {
        BaseRespDTO resp = cartService.deleteCartItem(httpRequest, request);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/items/checkout")
    public ResponseEntity<CheckoutCartRespDto> checkoutCart(HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(cartService.checkoutCart(request, response));
    }
}
