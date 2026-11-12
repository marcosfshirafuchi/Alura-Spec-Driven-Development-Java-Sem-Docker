package br.com.ecommerce.model;

import jakarta.persistence.*;

import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    public String id = UUID.randomUUID().toString();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    public PurchaseOrder order;
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    public Product product;
    public int quantity;
    @Column(precision = 10, scale = 2)
    public BigDecimal price;
}
