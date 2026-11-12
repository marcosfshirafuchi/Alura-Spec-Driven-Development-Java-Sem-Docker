package br.com.ecommerce.model;

import jakarta.persistence.*;

import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "orders")
public class PurchaseOrder {
    @Id
    public String id = UUID.randomUUID().toString();
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    public User user;
    @Enumerated(EnumType.STRING)
    public OrderStatus status = OrderStatus.PENDING;
    @Column(precision = 10, scale = 2)
    public BigDecimal totalValue = BigDecimal.ZERO;
    public String tenantId = "default";
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<OrderItem> items = new ArrayList<>();
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
}
