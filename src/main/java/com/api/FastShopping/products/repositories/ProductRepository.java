package com.api.FastShopping.products.repositories;

import com.api.FastShopping.products.models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query(value = """
    SELECT * FROM products
    WHERE name % :name
    OR name ILIKE '%' || :name || '%'
    ORDER BY similarity(name, :name) DESC
    """,
            countQuery = """
    SELECT COUNT(*) FROM products
    WHERE name % :name
    OR name ILIKE '%' || :name || '%'
    """,
            nativeQuery = true)
    Page<Product> fuzzySearch(@Param("name") String name, Pageable pageable);

    @Query("SELECT DISTINCT i.order.id FROM OrderItem i WHERE i.product.id = :productId")
    List<UUID> findOrderIdsByProductId(@Param("productId") UUID productId);
}
