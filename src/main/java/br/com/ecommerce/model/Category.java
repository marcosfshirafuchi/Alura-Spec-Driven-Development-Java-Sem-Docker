package br.com.ecommerce.model;

import jakarta.persistence.*;

import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    public String id = UUID.randomUUID().toString();
    public String name;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    public Category parent;
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
}
