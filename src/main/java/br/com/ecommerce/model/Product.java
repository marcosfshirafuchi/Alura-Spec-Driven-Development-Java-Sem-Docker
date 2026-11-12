package br.com.ecommerce.model;

import jakarta.persistence.*;

import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "products", indexes = @Index(columnList = "name"))
public class Product {
    @Id
    public String id = UUID.randomUUID().toString();
    public String name;
    @Column(length = 4000)
    public String description;
    @Column(unique = true, nullable = false)
    public String sku;
    @Column(precision = 10, scale = 2)
    public BigDecimal price;
    public int stock = 0;
    @Enumerated(EnumType.STRING)
    public ProductStatus status = ProductStatus.ACTIVE;
    public String tenantId = "default";
    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    public Category category;
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
}
