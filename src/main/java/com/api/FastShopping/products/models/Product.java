package com.api.FastShopping.products.models;

import com.api.FastShopping.products.dtos.ProductDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "products")
@Getter
@Setter
@RequiredArgsConstructor
public class Product {
    @Id
    @UuidGenerator
    private String id;

    @Column
    private String name;

    @Column
    private Float price;

    public Product(ProductDTO productDTO) {
        this.name = productDTO.name();
        this.price = productDTO.price();
    }
}
