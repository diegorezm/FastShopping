package com.api.FastShopping.products.repositories;

import com.api.FastShopping.products.models.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    @Query("SELECT o.id FROM Order o ORDER BY o.createdAt DESC")
    List<UUID> findPagedIds(Pageable pageable);

    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.product
        WHERE o.id IN :ids
        ORDER BY o.createdAt DESC
        """)
    List<Order> findWithItemsByIds(@Param("ids") List<UUID> ids);

    @Query("""
        SELECT o FROM Order o
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.product
        WHERE o.id = :id
        """)
    Optional<Order> findWithItemsById(@Param("id") UUID id);


}
