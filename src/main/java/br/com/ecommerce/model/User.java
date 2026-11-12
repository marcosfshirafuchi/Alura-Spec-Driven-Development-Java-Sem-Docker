package br.com.ecommerce.model;

import jakarta.persistence.*;

import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users")
public class User {
    @Id
    public String id = UUID.randomUUID().toString();
    public String name;
    @Column(unique = true, nullable = false)
    public String email;
    @JsonIgnore
    public String password;
    @Enumerated(EnumType.STRING)
    public Role role = Role.USER;
    public String tenantId = "default";
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
}
