package com.projects.marketmosaic.utils;

import com.projects.marketmosaic.dto.resp.CartDTO;
import com.projects.marketmosaic.dto.resp.CartItemDTO;
import com.projects.marketmosaic.entities.CartItem;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartUtils {
    public String getSession(HttpServletRequest request) {
        final String cookieName = "_guest_par";
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(cookieName)) {
                    return AESUtil.decrypt(cookie.getValue());
                }
            }
        }
        return null;
    }

    public void setSession(HttpServletResponse response, String sessionId) {
        Cookie sessionCookie = new Cookie("_guest_par", AESUtil.encrypt(sessionId));
        sessionCookie.setPath("/");
        sessionCookie.setHttpOnly(true);
        sessionCookie.setMaxAge(60 * 60 * 24); // 1 day
        response.addCookie(sessionCookie);
    }

    public String getCookie(HttpServletRequest request) {
        String jwtToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("JWT_SESSION".equals(cookie.getName())) {
                    jwtToken = cookie.getValue();
                    break;
                }
            }
        }

        if(StringUtils.isNotBlank(jwtToken)) {
            return "JWT_SESSION=" + jwtToken;
        }

        return null;
    }

    public CartDTO mapCartEntityToDTO(List<CartItem> cartItems) {
        CartDTO cartDTO = new CartDTO();
        AtomicReference<BigDecimal> totalPrice = new AtomicReference<>(BigDecimal.ZERO);
        List<CartItemDTO> cartItemDTOList = cartItems.stream().map(cartItem -> {
            CartItemDTO cartItemDTO = new CartItemDTO();
            cartItemDTO.setId(cartItem.getId());
            cartItemDTO.setPrice(cartItem.getPriceAtTime());
            cartItemDTO.setProductId(cartItem.getProductId());
            cartItemDTO.setQuantity(cartItem.getQuantity());
            cartItemDTO.setProductName(cartItem.getProductNameAtTime());
            cartItemDTO.setImageUrl(cartItem.getImageUrlAtTime());
            BigDecimal finalPrice = cartItem.getPriceAtTime().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalPrice.set(totalPrice.get().add(finalPrice));
            return cartItemDTO;
        }).toList();

        cartDTO.setTotalAmount(totalPrice.get());
        cartDTO.setItems(cartItemDTOList);
        return cartDTO;
    }
}
