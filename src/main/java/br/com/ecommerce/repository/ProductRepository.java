package br.com.ecommerce.repository;

import br.com.ecommerce.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

import org.springframework.data.domain.*;

public interface ProductRepository extends JpaRepository<Product, String> {
    Page<Product> findByNameContainingIgnoreCase(String search, Pageable pageable);
}
