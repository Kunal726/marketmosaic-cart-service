package com.projects.marketmosaic.repositories;

import com.projects.marketmosaic.entities.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
