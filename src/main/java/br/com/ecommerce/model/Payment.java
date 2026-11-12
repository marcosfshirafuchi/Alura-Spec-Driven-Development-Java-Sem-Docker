package br.com.ecommerce.model;

import jakarta.persistence.*;

import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    public String id = UUID.randomUUID().toString();
    @OneToOne(optional = false)
    @JoinColumn(name = "order_id", unique = true)
    public PurchaseOrder order;
    public String status = "AWAITING_CONFIRMATION";
    public String externalId;
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
}
