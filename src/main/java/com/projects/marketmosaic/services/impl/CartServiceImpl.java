package com.projects.marketmosaic.services.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.projects.marketmosaic.client.ProductClient;
import com.projects.marketmosaic.common.dto.product.resp.ProductDetailsDTO;
import com.projects.marketmosaic.common.dto.product.resp.ProductRespDTO;
import com.projects.marketmosaic.common.dto.resp.BaseRespDTO;
import com.projects.marketmosaic.common.exception.MarketMosaicCommonException;
import com.projects.marketmosaic.common.utils.CommonUtils;
import com.projects.marketmosaic.common.utils.RedisManager;
import com.projects.marketmosaic.common.utils.UserUtils;
import com.projects.marketmosaic.constants.Constants;
import com.projects.marketmosaic.dto.CheckoutCartRespDto;
import com.projects.marketmosaic.dto.resp.CartDTO;
import com.projects.marketmosaic.dto.req.AddToCartRequest;
import com.projects.marketmosaic.dto.req.DeleteCartItemRequest;
import com.projects.marketmosaic.dto.req.UpdateCartItemRequest;
import com.projects.marketmosaic.entities.CartEntity;
import com.projects.marketmosaic.entities.CartItem;
import com.projects.marketmosaic.repositories.CartItemRepository;
import com.projects.marketmosaic.repositories.CartRepository;
import com.projects.marketmosaic.services.CartService;
import com.projects.marketmosaic.utils.AESUtil;
import com.projects.marketmosaic.utils.CartUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final UserUtils userUtils;
    private final CartUtils cartUtils;
    private final RedisManager redisManager;
    private final CartRepository cartRepository;
    private final ProductClient productClient;
    private final CartItemRepository cartItemRepository;

    @Override
    public CartDTO getCart(HttpServletRequest request) {
        CartDTO cartDTO;
        Long userId = userUtils.getUserId(request);
        String sessionId = null;
        if(userId.equals(Constants.GUEST_USER_ID)) {
            sessionId = cartUtils.getSession(request);

            if (sessionId == null) {
                throw new MarketMosaicCommonException("Guest session not found");
            }
        }

        if(StringUtils.isNotBlank(sessionId)) {
            String guestUserCart = (String) redisManager.get(sessionId + Constants.USER_CART_SUFFIX);

            cartDTO = Optional.ofNullable(guestUserCart)
                    .filter(StringUtils::isNotBlank)
                    .map(json -> CommonUtils.fromJson(json, new TypeReference<List<CartItem>>() {}))
                    .map(cartItems -> {
                        CartDTO dto = cartUtils.mapCartEntityToDTO(cartItems);
                        getCartFetchedResp(dto);
                        return dto;
                    })
                    .orElseGet(this::getEmptyCartDTO);
        } else {
            CartEntity cartEntity = cartRepository.findByUserId(userId).orElse(null);

            cartDTO = Optional.ofNullable(cartEntity)
                    .map(entity -> {
                        CartDTO dto = cartUtils.mapCartEntityToDTO(entity.getItems());
                        dto.setId(AESUtil.encrypt(String.valueOf(entity.getId())));
                        getCartFetchedResp(dto);
                        return dto;
                    })
                    .orElseGet(this::getEmptyCartDTO);
        }

        return cartDTO;
    }

    @Override
    @Transactional
    public BaseRespDTO addToCart(HttpServletRequest request, HttpServletResponse response, AddToCartRequest addToCartRequest) {
        Long userId = userUtils.getUserId(request);
        String sessionId = null;
        if(userId.equals(Constants.GUEST_USER_ID)) {
            sessionId = cartUtils.getSession(request);

            if(sessionId == null) {
                sessionId = UUID.randomUUID().toString();
                cartUtils.setSession(response, sessionId);
            }
        }

        List<CartItem> cartItems;
        CartEntity cartEntity;

        if(StringUtils.isNotBlank(sessionId)) {
            String guestUserCart = (String) redisManager.get(sessionId + Constants.USER_CART_SUFFIX);
            cartItems = Optional.ofNullable(guestUserCart)
                    .filter(StringUtils::isNotBlank)
                    .map(json -> CommonUtils.fromJson(json, new TypeReference<List<CartItem>>() {}))
                    .orElse(new ArrayList<>());
            cartEntity = null;
        } else {
            CartEntity entity = cartRepository.findByUserId(userId).orElse(new CartEntity());
            cartItems = entity.getItems();
            cartEntity = entity;
        }

        ProductRespDTO productRespDTO = productClient.getProduct(String.valueOf(addToCartRequest.getProductId()), cartUtils.getCookie(request));

        if(productRespDTO == null || !productRespDTO.isStatus() || productRespDTO.getProduct() == null) {
            throw new MarketMosaicCommonException("Product Not found");
        }

        ProductDetailsDTO productDetailsDTO = productRespDTO.getProduct();

        if(productRespDTO.getProduct().getStockQuantity() < addToCartRequest.getQuantity()) {
            throw new MarketMosaicCommonException("Insufficient Stock Available");
        }

        CartItem cartItem = new CartItem();
        cartItem.setQuantity(addToCartRequest.getQuantity());
        cartItem.setProductId(addToCartRequest.getProductId());
        cartItem.setCart(cartEntity);
        cartItem.setPriceAtTime(productDetailsDTO.getPrice());
        cartItem.setProductNameAtTime(productDetailsDTO.getProductName());

        if(productDetailsDTO.getProductMedia() != null && !productDetailsDTO.getProductMedia().isEmpty()) {

            String url = productDetailsDTO.getProductMedia().stream()
                            .filter(productMedia -> productMedia.getType().contains("image"))
                            .map(ProductDetailsDTO.ProductMedia::getUrl)
                            .findFirst()
                            .orElse("");
            cartItem.setImageUrlAtTime(url);
        }

        if(cartEntity != null) {
            cartEntity.getItems().add(cartItem);
            cartEntity.setUserId(userId);
            cartRepository.save(cartEntity);
        } else {
            long uniqueId = System.currentTimeMillis() * 1000 + (long)(Math.random() * 1000);
            cartItem.setId(uniqueId);
            cartItems.add(cartItem);
            redisManager.set(sessionId + Constants.USER_CART_SUFFIX, CommonUtils.toJson(cartItems));
        }

        BaseRespDTO baseRespDTO  = new BaseRespDTO();
        baseRespDTO.setCode(String.valueOf(HttpStatus.OK.value()));
        baseRespDTO.setStatus(true);
        baseRespDTO.setMessage("Item added to cart");

        return baseRespDTO;
    }

    @Override
    @Transactional
    public BaseRespDTO updateCartItem(HttpServletRequest request, UpdateCartItemRequest updateCartItemRequest) {
        Long userId = userUtils.getUserId(request);
        String sessionId = null;
        if(userId.equals(Constants.GUEST_USER_ID)) {
            sessionId = cartUtils.getSession(request);
            if (sessionId == null) {
                throw new MarketMosaicCommonException("Guest session not found");
            }
        }

        CartItem cartItem;
        List<CartItem> cartItems = null;

        if(StringUtils.isNotBlank(sessionId)) {
            String guestUserCart = (String) redisManager.get(sessionId + Constants.USER_CART_SUFFIX);

            cartItems =  Optional.ofNullable(guestUserCart)
                    .filter(StringUtils::isNotBlank)
                    .map(json -> CommonUtils.fromJson(json, new TypeReference<List<CartItem>>() {}))
                    .orElse(new ArrayList<>());

            cartItem = cartItems
                    .stream()
                    .filter(item -> item.getId().equals(updateCartItemRequest.getCartItemId()))
                    .findFirst()
                    .orElseThrow(() -> new MarketMosaicCommonException("Cart Item Not Found"));
        } else {
             cartItem = cartItemRepository.findById(updateCartItemRequest.getCartItemId())
                    .orElseThrow(() -> new MarketMosaicCommonException("Cart Item Not Found"));
        }

        ProductRespDTO productRespDTO = productClient.getProduct(String.valueOf(cartItem.getProductId()), cartUtils.getCookie(request));

        if(productRespDTO == null || !productRespDTO.isStatus() || productRespDTO.getProduct() == null) {
            throw new MarketMosaicCommonException("Product Not found");
        }

        if(productRespDTO.getProduct().getStockQuantity() < updateCartItemRequest.getQuantity()) {
            throw new MarketMosaicCommonException("Insufficient Stock Available");
        }

        cartItem.setQuantity(updateCartItemRequest.getQuantity());

        if(StringUtils.isBlank(sessionId)) {
            cartItemRepository.save(cartItem);
        } else {
            redisManager.set(sessionId + Constants.USER_CART_SUFFIX, CommonUtils.toJson(cartItems));
        }

        BaseRespDTO baseRespDTO  = new BaseRespDTO();
        baseRespDTO.setCode(String.valueOf(HttpStatus.OK.value()));
        baseRespDTO.setStatus(true);
        baseRespDTO.setMessage("Cart Updated");

        return baseRespDTO;

    }

    @Override
    public BaseRespDTO deleteCartItem(HttpServletRequest request, DeleteCartItemRequest deleteCartItemRequest) {
        Long userId = userUtils.getUserId(request);
        String sessionId = null;
        if(userId.equals(Constants.GUEST_USER_ID)) {
            sessionId = cartUtils.getSession(request);
            if (sessionId == null) {
                throw new MarketMosaicCommonException("Guest session not found");
            }
        }

        if (StringUtils.isNotBlank(sessionId)) {
            String guestUserCartJson = (String) redisManager.get(sessionId + Constants.USER_CART_SUFFIX);
            List<CartItem> cartItems = Optional.ofNullable(guestUserCartJson)
                    .filter(StringUtils::isNotBlank)
                    .map(json -> CommonUtils.fromJson(json, new TypeReference<List<CartItem>>() {}))
                    .orElse(new ArrayList<>());

            boolean removed = cartItems.removeIf(item -> item.getId().equals(deleteCartItemRequest.getCartItemId()));

            if (!removed) {
                throw new MarketMosaicCommonException("Cart Item Not Found");
            }

            redisManager.set(sessionId + Constants.USER_CART_SUFFIX, CommonUtils.toJson(cartItems));

        } else {
            CartItem cartItem = cartItemRepository.findById(deleteCartItemRequest.getCartItemId())
                    .orElseThrow(() -> new MarketMosaicCommonException("Cart Item Not Found"));

            cartItemRepository.delete(cartItem);
        }

        // Return response
        BaseRespDTO response = new BaseRespDTO();
        response.setCode(String.valueOf(HttpStatus.OK.value()));
        response.setStatus(true);
        response.setMessage("Cart item deleted successfully");
        return response;
    }

    @Override
    @Transactional
    public CheckoutCartRespDto checkoutCart(HttpServletRequest request, HttpServletResponse response) {
        CheckoutCartRespDto respDTO = new CheckoutCartRespDto();

        Long userId = userUtils.getUserId(request);
        String sessionId = cartUtils.getSession(request);

        if(userId.equals(Constants.GUEST_USER_ID)) {
            respDTO.setStatus(true);
            respDTO.setCode(String.valueOf(HttpStatus.OK.value()));
            respDTO.setMessage("Please Login To Checkout");
            respDTO.setRedirectLogin(true);
            return respDTO;
        }

        CartEntity cartEntity = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CartEntity cart = new CartEntity();
                    cart.setUserId(userId);
                    return cart;
                });

        List<CartItem> dbCartItems = cartEntity.getItems();
        List<CartItem> guestCartItems = new ArrayList<>();

        if(StringUtils.isNotBlank(sessionId)) {
            String guestUserCartJson = (String) redisManager.get(sessionId + Constants.USER_CART_SUFFIX);
            guestCartItems = Optional.ofNullable(guestUserCartJson)
                    .filter(StringUtils::isNotBlank)
                    .map(json -> CommonUtils.fromJson(json, new TypeReference<List<CartItem>>() {}))
                    .orElse(new ArrayList<>());
        }

        Map<Long, CartItem> dbCartMap = dbCartItems.stream()
                .collect(Collectors.toMap(CartItem::getProductId, item -> item));

        guestCartItems.forEach(guestItem ->
                dbCartMap.compute(guestItem.getProductId(), (key, existing) -> {
                    if (existing != null) {
                        existing.setQuantity(existing.getQuantity() + guestItem.getQuantity());
                        return existing;
                    } else {
                        guestItem.setCart(cartEntity);
                        return guestItem;
                    }
                })
        );

        dbCartItems = new ArrayList<>(dbCartMap.values());

        if(dbCartItems.isEmpty()) {
            throw new MarketMosaicCommonException("Cart Empty");
        }

        List<String> productIds = dbCartItems.stream()
                .map(CartItem::getProductId)
                .map(String::valueOf)
                .toList();

        ProductRespDTO productRespDTO = productClient.getProducts(productIds, cartUtils.getCookie(request));

        if(!productRespDTO.isStatus() || productRespDTO.getProductList() == null) {
            throw new MarketMosaicCommonException("Something went wrong");
        }

        Map<Long, ProductDetailsDTO> productMap = productRespDTO.getProductList().stream()
                .collect(Collectors.toMap(ProductDetailsDTO::getProductId, Function.identity()));

        dbCartItems = dbCartItems.stream()
                .peek(item -> {
                    ProductDetailsDTO product = productMap.get(item.getProductId());

                    if (product == null) {
                        item.setOutOfStock(true);
                        return;
                    }

                    // Update price if changed
                    if (!item.getPriceAtTime().equals(product.getPrice())) {
                        item.setPriceAtTime(product.getPrice());
                    }

                    // Adjust quantity based on stock
                    if (item.getQuantity() > product.getStockQuantity()) {
                        item.setQuantity(product.getStockQuantity());
                        item.setOutOfStock(product.getStockQuantity() == 0);
                    } else {
                        item.setOutOfStock(false);
                    }

                    // Update name and image
                    item.setProductNameAtTime(product.getProductName());
                    if (product.getProductMedia() != null && !product.getProductMedia().isEmpty()) {
                        String url = product.getProductMedia().stream()
                                .filter(media -> media.getType().contains("image"))
                                .map(ProductDetailsDTO.ProductMedia::getUrl)
                                .findFirst()
                                .orElse("");
                        item.setImageUrlAtTime(url);
                    }
                })
                .collect(Collectors.toList());

        cartEntity.setItems(dbCartItems);
        cartRepository.save(cartEntity);
        if (StringUtils.isNotBlank(sessionId)) {
            redisManager.delete(sessionId + Constants.USER_CART_SUFFIX);
            cartUtils.deleteSession(response);
        }

        respDTO.setCode(String.valueOf(HttpStatus.OK.value()));
        respDTO.setStatus(true);
        respDTO.setMessage("Checkout Success");
        respDTO.setRedirectLogin(false);
        respDTO.setShowPlaceOrder(true);
        return respDTO;
    }


    private CartDTO getEmptyCartDTO() {
        CartDTO cartDTO = new CartDTO();
        cartDTO.setCode(String.valueOf(HttpStatus.NO_CONTENT.value()));
        cartDTO.setMessage("Cart is Empty");
        cartDTO.setStatus(false);
        return cartDTO;
    }

    private static void getCartFetchedResp(CartDTO cartDTO) {
        cartDTO.setStatus(true);
        cartDTO.setCode(String.valueOf(HttpStatus.OK.value()));
        cartDTO.setMessage("Cart Fetched Successfully");
    }
}
