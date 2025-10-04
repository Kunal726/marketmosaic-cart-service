package com.projects.marketmosaic.entities;

import io.micrometer.core.annotation.Counted;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "marketmosaic_cart_items")
@Data
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to parent Cart
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private CartEntity cart;

    // Store only the product ID — no entity relationship
    @Column(name = "product_id", nullable = false)
    private Long productId;

    // Snapshot fields for cart display and order creation
    @Column(name = "product_name_at_time", nullable = false)
    private String productNameAtTime;

    @Column(name = "price_at_time", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtTime;

    @Column(name = "image_url_at_time")
    private String imageUrlAtTime;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "out_of_Stock")
    private boolean outOfStock = false;
}
